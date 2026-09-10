package com.tvh.homestay.booking;

import com.tvh.homestay.availability.AvailabilityService;
import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import com.tvh.homestay.booking.dto.BookingDtos.BookingRoomView;
import com.tvh.homestay.booking.dto.BookingDtos.CreateBookingRequest;
import com.tvh.homestay.booking.dto.BookingDtos.PaymentStatusResponse;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingRoom;
import com.tvh.homestay.booking.entity.BookingRoomStatus;
import com.tvh.homestay.booking.exception.BookingExceptions.BookingNotFound;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidAccessToken;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidBookingRequest;
import com.tvh.homestay.booking.exception.BookingExceptions.RoomNotAvailable;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.booking.repository.BookingRoomRepository;
import com.tvh.homestay.common.SqlStates;
import com.tvh.homestay.booking.dto.BookingDtos.PaymentView;
import com.tvh.homestay.payment.PaymentService;
import com.tvh.homestay.payment.VietQrGenerator;
import com.tvh.homestay.payment.entity.Payment;
import com.tvh.homestay.promotion.PromotionService;
import com.tvh.homestay.promotion.entity.Promotion;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.repository.RoomTypeRepository;
import com.tvh.homestay.user.entity.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Điều phối việc tạo, tra cứu và xem trạng thái đơn. */
@Service
public class BookingService {

    private final AvailabilityService availabilityService;
    private final RoomAllocator allocator;
    private final BookingTxService bookingTx;
    private final BookingPricingService pricing;
    private final PromotionService promotions;
    private final RoomTypeRepository roomTypes;
    private final BookingRepository bookings;
    private final BookingRoomRepository bookingRooms;
    private final PaymentService payments;
    private final VietQrGenerator qr;

    public BookingService(
            AvailabilityService availabilityService,
            RoomAllocator allocator,
            BookingTxService bookingTx,
            BookingPricingService pricing,
            PromotionService promotions,
            RoomTypeRepository roomTypes,
            BookingRepository bookings,
            BookingRoomRepository bookingRooms,
            PaymentService payments,
            VietQrGenerator qr) {
        this.availabilityService = availabilityService;
        this.allocator = allocator;
        this.bookingTx = bookingTx;
        this.pricing = pricing;
        this.promotions = promotions;
        this.roomTypes = roomTypes;
        this.bookings = bookings;
        this.bookingRooms = bookingRooms;
        this.payments = payments;
        this.qr = qr;
    }

    /**
     * Tạo đơn và giữ chỗ.
     *
     * <p>Việc chọn và gán phòng nằm ở {@link RoomAllocator} — đọc javadoc ở đó
     * để biết vì sao vòng thử không thể nằm trong một transaction.
     */
    public BookingResponse create(
            CreateBookingRequest request, User user, String clientIp, String userAgent) {

        availabilityService.validateDates(request.checkIn(), request.checkOut());
        RoomType roomType = roomTypes.findById(request.roomTypeId())
                .orElseThrow(() -> new InvalidBookingRequest("Không tìm thấy loại phòng."));

        int nights = AvailabilityService.nights(request.checkIn(), request.checkOut());
        int adultsPerRoom = AvailabilityService.perRoom(request.adults(), request.roomQuantity());
        int childrenPerRoom = AvailabilityService.perRoom(request.children(), request.roomQuantity());
        if (adultsPerRoom > roomType.getCapacityAdults()
                || childrenPerRoom > roomType.getCapacityChildren()) {
            throw new InvalidBookingRequest("Số khách vượt sức chứa của loại phòng đã chọn.");
        }

        BigDecimal subtotalBeforePromotion = roomType.getBasePrice()
                .multiply(BigDecimal.valueOf((long) nights * request.roomQuantity()));
        Promotion promotion =
                promotions.validate(request.promotionCode(), nights, subtotalBeforePromotion);
        BookingPricingService.Quote quote = pricing.quote(
                roomType.getBasePrice(), nights, request.roomQuantity(), promotion);

        BookingTxService.Attempt attempt = new BookingTxService.Attempt(
                request, roomType, promotion, quote, nights, user, clientIp, userAgent);

        Booking booking = allocator.allocate(
                request.roomTypeId(),
                request.checkIn(),
                request.checkOut(),
                request.roomQuantity(),
                picked -> bookingTx.createInNewTransaction(attempt, picked));

        // Chỉ tiêu thụ lượt khuyến mãi khi đơn đã ghi xong. Tiêu thụ trước rồi
        // lần thử hỏng là đốt lượt của một đơn không tồn tại.
        promotions.consume(promotion);
        return toResponse(booking, true);
    }

    /** Tra cứu bằng mã và số điện thoại. Sai một trong hai đều trả "không tìm thấy". */
    @Transactional(readOnly = true)
    public BookingResponse lookup(String code, String phone) {
        Booking booking = bookings.findByCode(normalizeCode(code)).orElseThrow(BookingNotFound::new);
        if (!booking.getGuestPhone().equals(phone == null ? null : phone.trim())) {
            throw new BookingNotFound();
        }
        return toResponse(booking, true);
    }

    /** Tìm đơn theo mã và kiểm mã truy cập. Thiếu hoặc sai token là 401. */
    @Transactional(readOnly = true)
    public Booking requireByCodeAndToken(String code, String accessToken) {
        Booking booking = bookings.findByCode(normalizeCode(code)).orElseThrow(BookingNotFound::new);
        if (accessToken == null || !accessToken.equals(booking.getAccessToken())) {
            throw new InvalidAccessToken();
        }
        return booking;
    }

    /**
     * Tìm đơn cho thao tác huỷ: chấp nhận mã truy cập HOẶC số điện thoại.
     *
     * <p>Số điện thoại là lối đi cho khách đã mất link; nó yếu hơn token nên
     * chỉ dùng được cho những thao tác khách tự làm với đơn của chính mình.
     */
    @Transactional(readOnly = true)
    public Booking requireByCodeAndTokenOrPhone(String code, String accessToken, String phone) {
        Booking booking = bookings.findByCode(normalizeCode(code)).orElseThrow(BookingNotFound::new);
        boolean tokenOk = accessToken != null && accessToken.equals(booking.getAccessToken());
        boolean phoneOk = phone != null && booking.getGuestPhone().equals(phone.trim());
        if (!tokenOk && !phoneOk) {
            throw new BookingNotFound();
        }
        return booking;
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse paymentStatus(String code, String accessToken) {
        Booking booking = requireByCodeAndToken(code, accessToken);
        Payment latest = payments.latestFor(booking.getId()).orElse(null);
        return new PaymentStatusResponse(
                booking.getCode(),
                booking.getStatus().name(),
                booking.getPaymentStatus().name(),
                latest == null ? BigDecimal.ZERO : latest.getAmountExpected(),
                latest == null ? BigDecimal.ZERO : latest.getAmountReceived(),
                booking.getHoldExpiresAt());
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listForUser(Long userId, Pageable pageable) {
        return bookings.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(booking -> toResponse(booking, false));
    }

    @Transactional(readOnly = true)
    public BookingResponse getForUser(String code, Long userId) {
        return bookings.findByCodeAndUserId(normalizeCode(code), userId)
                .map(booking -> toResponse(booking, true))
                .orElseThrow(BookingNotFound::new);
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    /**
     * @param includeSecret có kèm {@code accessToken} hay không. Danh sách đơn
     *     KHÔNG kèm: mã truy cập chỉ nên xuất hiện đúng lúc khách cần nó.
     */
    public BookingResponse toResponse(Booking booking, boolean includeSecret) {
        List<BookingRoom> assigned =
                bookingRooms.findWithRoomByBookingIdAndStatus(booking.getId(), BookingRoomStatus.ACTIVE);
        PaymentView paymentView = payments.latestFor(booking.getId())
                .map(payment -> new PaymentView(
                        payment.getTransferContent(),
                        payment.getQrImageUrl(),
                        qr.getAccountNumber(),
                        qr.getBankCode(),
                        payment.getAmountExpected(),
                        payment.getExpiresAt()))
                .orElse(null);

        return new BookingResponse(
                booking.getCode(),
                includeSecret ? booking.getAccessToken() : null,
                booking.getStatus().name(),
                booking.getPaymentStatus().name(),
                booking.getRoomTypeNameSnapshot(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                AvailabilityService.nights(booking.getCheckIn(), booking.getCheckOut()),
                booking.getRoomQuantity(),
                booking.getAdults(),
                booking.getChildren(),
                booking.getGuestName(),
                booking.getGuestPhone(),
                booking.getUnitPriceSnapshot(),
                booking.getSubtotalAmount(),
                booking.getDiscountAmount(),
                booking.getTotalAmount(),
                booking.getDepositAmount(),
                paymentView,
                booking.getHoldExpiresAt(),
                assigned.stream()
                        .map(br -> new BookingRoomView(
                                br.getRoom().getId(),
                                br.getRoom().getRoomNumber(),
                                br.getCheckIn(),
                                br.getCheckOut()))
                        .toList());
    }
}
