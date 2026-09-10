package com.tvh.homestay.booking;

import com.tvh.homestay.availability.AvailabilityRepository;
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
import com.tvh.homestay.payment.entity.Payment;
import com.tvh.homestay.payment.repository.PaymentRepository;
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

    /**
     * Số lần thử gán phòng tối đa.
     *
     * <p>Mỗi lần thử đã loại bỏ các phòng vừa va chạm, và danh sách ứng viên
     * được lọc lại từ đầu, nên năm lần là quá đủ ngay cả lúc cao điểm.
     */
    private static final int MAX_ATTEMPTS = 5;

    private final AvailabilityService availabilityService;
    private final AvailabilityRepository availabilityRepository;
    private final BookingTxService bookingTx;
    private final BookingPricingService pricing;
    private final PromotionService promotions;
    private final RoomTypeRepository roomTypes;
    private final BookingRepository bookings;
    private final BookingRoomRepository bookingRooms;
    private final PaymentRepository payments;

    public BookingService(
            AvailabilityService availabilityService,
            AvailabilityRepository availabilityRepository,
            BookingTxService bookingTx,
            BookingPricingService pricing,
            PromotionService promotions,
            RoomTypeRepository roomTypes,
            BookingRepository bookings,
            BookingRoomRepository bookingRooms,
            PaymentRepository payments) {
        this.availabilityService = availabilityService;
        this.availabilityRepository = availabilityRepository;
        this.bookingTx = bookingTx;
        this.pricing = pricing;
        this.promotions = promotions;
        this.roomTypes = roomTypes;
        this.bookings = bookings;
        this.bookingRooms = bookingRooms;
        this.payments = payments;
    }

    /**
     * Tạo đơn và giữ chỗ.
     *
     * <h2>KHÔNG ĐƯỢC ĐẶT {@code @Transactional} LÊN PHƯƠNG THỨC NÀY</h2>
     *
     * <p>Vòng thử bên dưới bắt lỗi va chạm ràng buộc chống trùng lịch rồi thử
     * bộ phòng khác. Gộp nó vào một transaction là bất khả thi ở cả ba tầng,
     * không phải một lựa chọn kém:
     *
     * <ul>
     *   <li><b>PostgreSQL:</b> mọi lỗi đưa transaction vào trạng thái aborted.
     *       Lệnh kế tiếp trả {@code 25P02 current transaction is aborted} chứ
     *       KHÔNG phải {@code 23P01} — nghĩa là lần thử thứ hai không bao giờ
     *       chạy, và mã lỗi bắt được cũng không còn là mã ta đang chờ.
     *   <li><b>Spring:</b> {@code DataIntegrityViolationException} là unchecked
     *       nên transaction bị đánh dấu rollback-only. Bắt lại rồi chạy tiếp sẽ
     *       ném {@code UnexpectedRollbackException} lúc commit — tức là lần thử
     *       THÀNH CÔNG cuối cùng cũng bị vứt đi.
     *   <li><b>JPA:</b> sau khi provider ném ngoại lệ, {@code EntityManager} ở
     *       trạng thái không xác định và phải bị huỷ, không được dùng lại để
     *       persist.
     * </ul>
     *
     * <p>Triệu chứng nếu ai đó gộp lại: khách nhận HTTP 500 <b>dù loại phòng
     * còn phòng trống</b>. Test {@code BookingConcurrencyIT#multiRoom} tồn tại
     * để bắt đúng lỗi này — với loại phòng chỉ có một phòng, vòng thử thoát
     * ngay ở thất bại đầu tiên nên thiết kế sai vẫn xanh.
     *
     * <p>Ràng buộc {@code EXCLUDE} không hỗ trợ {@code ON CONFLICT}, nên chỉ có
     * hai lối đúng: savepoint cho mỗi lần thử, hoặc mỗi lần thử là một
     * transaction mới. Ở đây chọn lối thứ hai — không phụ thuộc
     * {@code nestedTransactionAllowed} và dễ đọc hơn.
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

        Set<Long> excluded = new HashSet<>();
        for (int round = 1; round <= MAX_ATTEMPTS; round++) {
            List<Long> candidates = new ArrayList<>(availabilityRepository.findFreeRoomIds(
                    request.roomTypeId(), request.checkIn(), request.checkOut()));
            candidates.removeAll(excluded);
            if (candidates.size() < request.roomQuantity()) {
                throw new RoomNotAvailable();
            }
            List<Long> picked = List.copyOf(candidates.subList(0, request.roomQuantity()));

            try {
                Booking booking = bookingTx.createInNewTransaction(attempt, picked);
                // Chỉ tiêu thụ lượt khuyến mãi khi đơn đã ghi xong. Tiêu thụ
                // trước rồi lần thử hỏng là đốt lượt của một đơn không tồn tại.
                promotions.consume(promotion);
                return toResponse(booking, true);
            } catch (DataIntegrityViolationException e) {
                // CHỈ 23P01 mới là "phòng vừa bị người khác lấy mất". Mọi mã
                // khác — khoá ngoại, NOT NULL, trigger bất biến — là bug thật
                // và phải nổi lên nguyên trạng, không được che thành "hết phòng".
                if (!SqlStates.isExclusionViolation(e)) {
                    throw e;
                }
                excluded.addAll(picked);
            }
        }
        throw new RoomNotAvailable();
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
        List<Payment> attempts = payments.findByBookingIdOrderByAttemptNoAsc(booking.getId());
        Payment latest = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
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
        String transferContent = payments.findByBookingIdOrderByAttemptNoAsc(booking.getId()).stream()
                .reduce((first, second) -> second)
                .map(Payment::getTransferContent)
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
                transferContent,
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
