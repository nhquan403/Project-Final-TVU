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

    /** Trạng thái kết thúc: đơn không đi tiếp được, và phòng phải được trả lại. */
    private static final EnumSet<BookingStatus> TERMINAL = EnumSet.of(
            BookingStatus.CANCELLED, BookingStatus.EXPIRED, BookingStatus.NO_SHOW,
            BookingStatus.CHECKED_OUT);

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
            BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class),
            BookingStatus.EXPIRED, EnumSet.noneOf(BookingStatus.class),
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
        entry.setNote(reason);
        history.save(entry);

        if (TERMINAL.contains(target)) {
            // Nhả phòng NGAY trong transaction này. Ràng buộc chống trùng chỉ
            // soi dòng ACTIVE, nên slot mở lại tức thì cho khách khác.
            bookingRooms.releaseByBookingIds(List.of(booking.getId()));

            // Hoàn lượt khuyến mãi, trừ khi đơn đã đi tới cuối hành trình:
            // khách CHECKED_OUT là đã thật sự dùng mã đó.
            if (target != BookingStatus.CHECKED_OUT) {
                promotions.release(booking.getPromotion());
            }
        }
        return booking;
    }
}
