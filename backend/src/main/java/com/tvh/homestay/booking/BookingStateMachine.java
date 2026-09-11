package com.tvh.homestay.booking;

import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.BookingStatusHistory;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidStateTransition;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.booking.repository.BookingRoomRepository;
import com.tvh.homestay.booking.repository.BookingStatusHistoryRepository;
import com.tvh.homestay.promotion.PromotionService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nơi DUY NHẤT được đổi trạng thái đơn.
 *
 * <p>Gom vào một chỗ vì mỗi lần chuyển kéo theo ba việc phụ mà rải rác ở
 * controller thì chắc chắn có chỗ quên: ghi nhật ký, nhả phòng khi vào trạng
 * thái kết thúc, và hoàn lượt khuyến mãi. Quên việc thứ hai là giam phòng vĩnh
 * viễn; quên việc thứ ba là đốt oan lượt của mã giảm giá.
 */
@Service
public class BookingStateMachine {

    /**
     * Trạng thái mà chuyến đi KHÔNG diễn ra — và chỉ khi đó phòng mới được trả
     * về kho cùng lượt khuyến mãi.
     *
     * <p><b>{@code CHECKED_OUT} cố ý KHÔNG nằm ở đây, dù nó cũng là trạng thái
     * kết thúc.</b> Khách đã ở thật, nên những dòng {@code booking_rooms} của
     * đơn đó là BẰNG CHỨNG LỊCH SỬ về số đêm-phòng đã bán, không phải chỗ đang
     * bị giữ. Nhả chúng gây hỏng theo hai hướng cùng lúc:
     *
     * <ul>
     *   <li>Ràng buộc ở tầng cơ sở dữ liệu bác ngay: hàm
     *       {@code assert_booking_room_count} (V3) đòi số dòng ACTIVE khớp
     *       {@code room_quantity} với mọi đơn KHÔNG thuộc
     *       {@code CANCELLED/EXPIRED/NO_SHOW} — danh sách đó không có
     *       {@code CHECKED_OUT}, và đây chính là lý do.
     *   <li>Tỉ lệ lấp đầy tính theo dòng ACTIVE của đơn
     *       {@code CONFIRMED/CHECKED_IN/CHECKED_OUT}; nhả phòng khi trả phòng
     *       sẽ đưa số liệu của mọi chuyến đã hoàn tất về 0.
     * </ul>
     *
     * <p>Không nhả phòng ở đây cũng không giam phòng: ràng buộc chống trùng chỉ
     * so KHOẢNG NGÀY, mà khoảng ngày của một chuyến đã trả phòng nằm trong quá
     * khứ nên không chặn ai.
     */
    private static final EnumSet<BookingStatus> RELEASES_ROOMS = EnumSet.of(
            BookingStatus.CANCELLED, BookingStatus.EXPIRED, BookingStatus.NO_SHOW);

    /** Bảng chuyển trạng thái hợp lệ, khớp sơ đồ trong kế hoạch. */
    private static final Map<BookingStatus, EnumSet<BookingStatus>> ALLOWED = Map.of(
            BookingStatus.PENDING_PAYMENT, EnumSet.of(
                    BookingStatus.CONFIRMED, BookingStatus.AWAITING_REVIEW,
                    BookingStatus.EXPIRED, BookingStatus.CANCELLED),
            BookingStatus.AWAITING_REVIEW, EnumSet.of(
                    BookingStatus.CONFIRMED, BookingStatus.CANCELLED),
            BookingStatus.CONFIRMED, EnumSet.of(
                    BookingStatus.CHECKED_IN, BookingStatus.CANCELLED, BookingStatus.NO_SHOW),
            BookingStatus.CHECKED_IN, EnumSet.of(BookingStatus.CHECKED_OUT),
            BookingStatus.CHECKED_OUT, EnumSet.noneOf(BookingStatus.class),
            // CANCELLED và EXPIRED có ĐÚNG MỘT đường ra: sang AWAITING_REVIEW,
            // và chỉ khi tiền của khách về sau khi đơn đã đóng.
            //
            // Cửa sổ đua là có thật: ngân hàng → nhà cung cấp → API trễ vài
            // chục giây, còn đồng hồ đếm ngược trên màn hình QR lại đẩy khách
            // bấm chuyển khoản vào đúng phút cuối. Không có cạnh này thì nhánh
            // đó kết thúc bằng "tiền đã vào tài khoản, đơn đã đóng, không ai
            // biết" — đúng cái mà cả thiết kế này sinh ra để ngăn.
            //
            // Không mở thẳng sang CONFIRMED: phòng đã được nhả cho khách khác,
            // nên phải qua AWAITING_REVIEW rồi mới thử gán lại. Gán được thì
            // AWAITING_REVIEW → CONFIRMED (đã có sẵn ở trên); không gán được
            // thì dừng ở đó cho người xử lý.
            BookingStatus.CANCELLED, EnumSet.of(BookingStatus.AWAITING_REVIEW),
            BookingStatus.EXPIRED, EnumSet.of(BookingStatus.AWAITING_REVIEW),
            BookingStatus.NO_SHOW, EnumSet.noneOf(BookingStatus.class));

    private final BookingRepository bookings;
    private final BookingRoomRepository bookingRooms;
    private final BookingStatusHistoryRepository history;
    private final PromotionService promotions;
    private final Clock clock;

    public BookingStateMachine(
            BookingRepository bookings,
            BookingRoomRepository bookingRooms,
            BookingStatusHistoryRepository history,
            PromotionService promotions,
            Clock clock) {
        this.bookings = bookings;
        this.bookingRooms = bookingRooms;
        this.history = history;
        this.promotions = promotions;
        this.clock = clock;
    }

    public static boolean canTransition(BookingStatus from, BookingStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(BookingStatus.class)).contains(to);
    }

    /**
     * Chuyển trạng thái, kèm mọi hệ quả, TRONG CÙNG một transaction.
     *
     * @param reason ghi vào nhật ký; với huỷ thì cũng ghi vào {@code cancel_reason}
     */
    @Transactional
    public Booking transition(
            Booking booking, BookingStatus target, HistoryActor actor, String reason) {
        return transition(booking, target, actor, null, reason);
    }

    /**
     * Bản có ghi NGƯỜI thực hiện.
     *
     * <p>Với thao tác của quản trị viên, {@code actor = ADMIN} mới chỉ nói
     * "một người nào đó có quyền". Cột {@code changed_by} nói người nào — và đó
     * là khác biệt giữa một dòng nhật ký và một dòng nhật ký dùng được khi phải
     * truy lại ai đã huỷ đơn của khách.
     */
    @Transactional
    public Booking transition(
            Booking booking,
            BookingStatus target,
            HistoryActor actor,
            com.tvh.homestay.user.entity.User changedBy,
            String reason) {

        BookingStatus current = booking.getStatus();
        if (!canTransition(current, target)) {
            throw new InvalidStateTransition(current.name(), target.name());
        }

        booking.setStatus(target);
        if (target == BookingStatus.CANCELLED) {
            booking.setCancelledAt(OffsetDateTime.now(clock));
            booking.setCancelReason(reason);
        }
        bookings.save(booking);

        BookingStatusHistory entry = new BookingStatusHistory();
        entry.setBooking(booking);
        entry.setFromStatus(current);
        entry.setToStatus(target);
        entry.setActor(actor);
        entry.setChangedBy(changedBy);
        entry.setNote(reason);
        history.save(entry);

        if (RELEASES_ROOMS.contains(target)) {
            // Nhả phòng NGAY trong transaction này. Ràng buộc chống trùng chỉ
            // soi dòng ACTIVE, nên slot mở lại tức thì cho khách khác.
            bookingRooms.releaseByBookingIds(List.of(booking.getId()));

            // Và hoàn lượt khuyến mãi: chuyến đi không diễn ra thì mã chưa
            // thực sự được dùng.
            promotions.release(booking.getPromotion());
        }
        return booking;
    }
}
