package com.tvh.homestay.booking;

import com.tvh.homestay.booking.dto.BookingDtos.CreateBookingRequest;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingRoom;
import com.tvh.homestay.booking.entity.BookingRoomStatus;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.BookingStatusHistory;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.booking.repository.BookingRoomRepository;
import com.tvh.homestay.booking.repository.BookingStatusHistoryRepository;
import com.tvh.homestay.payment.PaymentService;
import com.tvh.homestay.promotion.entity.Promotion;
import com.tvh.homestay.room.entity.Room;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.repository.RoomRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * MỘT lần thử gán phòng, trong MỘT transaction hoàn toàn mới.
 *
 * <p>Tách khỏi {@link BookingService} không phải để chia nhỏ cho đẹp: nó là
 * điều kiện để vòng thử chạy được. Xem javadoc của
 * {@link BookingService#create} để biết vì sao vòng thử không thể nằm trong
 * cùng transaction.
 *
 * <p>{@code REQUIRES_NEW} bảo đảm mỗi lần thử có transaction riêng, nên một
 * lần thử hỏng không kéo theo lần thử sau, và transaction hỏng được đóng lại
 * dứt điểm trước khi lần sau bắt đầu.
 */
@Service
public class BookingTxService {

    private final BookingRepository bookings;
    private final BookingRoomRepository bookingRooms;
    private final BookingStatusHistoryRepository history;
    private final PaymentService payments;
    private final RoomRepository rooms;
    private final BookingCodeGenerator codes;
    private final Clock clock;
    private final Duration holdDuration;

    public BookingTxService(
            BookingRepository bookings,
            BookingRoomRepository bookingRooms,
            BookingStatusHistoryRepository history,
            PaymentService payments,
            RoomRepository rooms,
            BookingCodeGenerator codes,
            @Value("${booking.hold-minutes:15}") long holdMinutes,
            Clock clock) {
        this.bookings = bookings;
        this.bookingRooms = bookingRooms;
        this.history = history;
        this.payments = payments;
        this.rooms = rooms;
        this.codes = codes;
        this.holdDuration = Duration.ofMinutes(holdMinutes);
        this.clock = clock;
    }

    /** Dữ liệu đã tính sẵn ở ngoài, để lần thử chỉ còn việc ghi. */
    public record Attempt(
            CreateBookingRequest request,
            RoomType roomType,
            Promotion promotion,
            BookingPricingService.Quote quote,
            int nights,
            com.tvh.homestay.user.entity.User user,
            String clientIp,
            String userAgent) {}

    /**
     * Ghi đơn, gán ĐỦ số phòng, và tạo lần thanh toán đầu tiên.
     *
     * <p>All-or-nothing: một transaction gán đủ {@code roomQuantity} phòng hoặc
     * rollback toàn bộ. Không bao giờ commit một phần — và constraint trigger
     * {@code booking_room_count_check} ở tầng cơ sở dữ liệu là lưới chắn cuối
     * nếu tầng này có sai sót.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException khi đụng
     *     ràng buộc chống trùng lịch; nơi gọi kiểm SQLSTATE để quyết định thử lại
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Booking createInNewTransaction(Attempt attempt, List<Long> roomIds) {
        CreateBookingRequest request = attempt.request();
        OffsetDateTime now = OffsetDateTime.now(clock);

        Booking booking = new Booking();
        booking.setCode(uniqueCode());
        booking.setAccessToken(codes.newAccessToken());
        booking.setUser(attempt.user());
        booking.setGuestName(request.guestName().trim());
        booking.setGuestEmail(request.guestEmail().trim().toLowerCase());
        booking.setGuestPhone(request.guestPhone().trim());
        booking.setCheckIn(request.checkIn());
        booking.setCheckOut(request.checkOut());
        booking.setAdults(request.adults());
        booking.setChildren(request.children());
        booking.setRoomType(attempt.roomType());
        booking.setRoomTypeNameSnapshot(attempt.roomType().getName());
        booking.setUnitPriceSnapshot(attempt.quote().unitPrice());
        booking.setRoomQuantity(request.roomQuantity());
        booking.setSubtotalAmount(attempt.quote().subtotal());
        booking.setDiscountAmount(attempt.quote().discount());
        booking.setTotalAmount(attempt.quote().total());
        booking.setDepositAmount(attempt.quote().deposit());
        booking.setPromotion(attempt.promotion());
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setSpecialRequest(request.specialRequest());
        booking.setHoldExpiresAt(now.plus(holdDuration));
        booking.setClientIp(attempt.clientIp());
        booking.setUserAgent(attempt.userAgent());
        bookings.saveAndFlush(booking);

        for (Long roomId : roomIds) {
            Room room = rooms.getReferenceById(roomId);
            BookingRoom bookingRoom = new BookingRoom();
            bookingRoom.setBooking(booking);
            bookingRoom.setRoom(room);
            bookingRoom.setCheckIn(request.checkIn());
            bookingRoom.setCheckOut(request.checkOut());
            bookingRoom.setStatus(BookingRoomStatus.ACTIVE);
            bookingRooms.save(bookingRoom);
        }
        // Đẩy xuống cơ sở dữ liệu NGAY trong lần thử này. Không flush thì va
        // chạm ràng buộc chống trùng chỉ lộ ra lúc commit — muộn hơn nhiều so
        // với chỗ vòng thử cần biết.
        bookingRooms.flush();

        // Lần thanh toán đầu tiên do PaymentService dựng — kèm nội dung chuyển
        // khoản và ảnh QR. Một đường tạo duy nhất, để định dạng nội dung chuyển
        // khoản không bao giờ lệch giữa hai chỗ.
        payments.createForBooking(booking);

        BookingStatusHistory entry = new BookingStatusHistory();
        entry.setBooking(booking);
        entry.setFromStatus(null);
        entry.setToStatus(BookingStatus.PENDING_PAYMENT);
        entry.setActor(attempt.user() == null ? HistoryActor.GUEST : HistoryActor.CUSTOMER);
        entry.setNote("Tạo đơn và giữ chỗ");
        history.save(entry);

        return booking;
    }

    /**
     * Gán LẠI phòng cho một đơn đã có sẵn, trong MỘT transaction hoàn toàn mới.
     *
     * <p>Dùng cho nhánh tiền về muộn: đơn đã {@code EXPIRED}/{@code CANCELLED}
     * nên phòng đã bị nhả, và giờ phải giành lại đủ số phòng cũ. Chạy qua
     * {@link RoomAllocator} giống hệt lúc tạo đơn — cùng một vòng thử, cùng một
     * cách bắt va chạm ràng buộc chống trùng.
     *
     * <p><b>Chỉ chèn {@code booking_rooms}, tuyệt đối không đụng vào dòng
     * {@code bookings}.</b> Transaction gọi vào đây đang giữ khoá trên đúng
     * dòng đó (xem {@code BookingRepository#lockById}); một lệnh UPDATE ở đây
     * sẽ chờ transaction ngoài, còn transaction ngoài lại đang chờ transaction
     * này — treo cho tới khi hết {@code lock_timeout}, và PostgreSQL không coi
     * đó là deadlock nên không tự gỡ.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException khi đụng
     *     ràng buộc chống trùng lịch; {@link RoomAllocator} bắt và thử bộ khác
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reassignInNewTransaction(Long bookingId, List<Long> roomIds) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalStateException("Không còn đơn " + bookingId));
        for (Long roomId : roomIds) {
            BookingRoom bookingRoom = new BookingRoom();
            bookingRoom.setBooking(booking);
            bookingRoom.setRoom(rooms.getReferenceById(roomId));
            bookingRoom.setCheckIn(booking.getCheckIn());
            bookingRoom.setCheckOut(booking.getCheckOut());
            bookingRoom.setStatus(BookingRoomStatus.ACTIVE);
            bookingRooms.save(bookingRoom);
        }
        bookingRooms.flush();
    }

    /**
     * Sinh mã chưa bị trùng.
     *
     * <p>Cột {@code code} có UNIQUE, nên đây chỉ là lớp giảm xác suất va chạm;
     * chốt chặn thật vẫn là ràng buộc ở cơ sở dữ liệu.
     */
    private String uniqueCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = codes.newCode();
            if (!bookings.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã đơn duy nhất sau 5 lần thử");
    }
}
