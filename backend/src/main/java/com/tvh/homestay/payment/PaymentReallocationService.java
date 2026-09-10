package com.tvh.homestay.payment;

import com.tvh.homestay.booking.BookingStateMachine;
import com.tvh.homestay.booking.BookingTxService;
import com.tvh.homestay.booking.RoomAllocator;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.exception.BookingExceptions.RoomNotAvailable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhánh cứu tiền của khách: tiền về SAU khi đơn đã đóng.
 *
 * <p>Cửa sổ đua là có thật. Ngân hàng → nhà cung cấp → API thường trễ 5–30
 * giây, và đồng hồ đếm ngược trên màn hình QR lại đẩy khách bấm chuyển khoản
 * vào đúng phút cuối. Không có nhánh này, kịch bản đó kết thúc bằng: tiền đã
 * vào tài khoản, phòng đã bán cho người khác, và không bản ghi nào cho thấy
 * chuyện gì đã xảy ra.
 *
 * <p>Chính sách đã chốt: <b>tự gán lại phòng cùng loại; hết phòng thì chuyển
 * người xử lý</b> — không tự ý hoàn tiền, không tự ý đổi ngày. Cả hai việc đó
 * đều là quyết định kinh doanh, không phải quyết định của một bộ xử lý webhook.
 *
 * <p>Đường đi bắt buộc là {@code EXPIRED/CANCELLED → AWAITING_REVIEW → CONFIRMED}
 * chứ không nhảy thẳng sang {@code CONFIRMED}: lúc đơn đóng, phòng đã được nhả
 * ra cho khách khác, nên một đơn {@code CONFIRMED} mà không giữ phòng nào là
 * đúng cái trạng thái mà cả thiết kế này sinh ra để ngăn.
 */
@Service
public class PaymentReallocationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReallocationService.class);

    private final RoomAllocator allocator;
    private final BookingTxService bookingTx;
    private final BookingStateMachine stateMachine;

    public PaymentReallocationService(
            RoomAllocator allocator, BookingTxService bookingTx, BookingStateMachine stateMachine) {
        this.allocator = allocator;
        this.bookingTx = bookingTx;
        this.stateMachine = stateMachine;
    }

    /**
     * Đưa đơn đã đóng về {@code AWAITING_REVIEW} rồi thử giành lại phòng.
     *
     * <p>Chạy trong transaction của bộ xử lý webhook ({@code MANDATORY}) để
     * việc ghi nhận tiền và việc đổi trạng thái đơn cùng sống hoặc cùng chết.
     * Riêng lượt chèn {@code booking_rooms} đi qua transaction con
     * {@code REQUIRES_NEW} — đó là điều kiện để vòng thử lại chạy được sau một
     * va chạm ràng buộc; xem javadoc của {@link RoomAllocator}.
     *
     * @return {@code true} nếu giành lại đủ phòng. Người gọi chịu trách nhiệm
     *     chuyển tiếp sang {@code CONFIRMED}; {@code false} thì đơn dừng ở
     *     {@code AWAITING_REVIEW} cho người xử lý.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean reassign(Booking booking) {
        if (booking.getStatus() != BookingStatus.AWAITING_REVIEW) {
            stateMachine.transition(
                    booking,
                    BookingStatus.AWAITING_REVIEW,
                    HistoryActor.SYSTEM,
                    "Tiền về sau khi đơn đã đóng — đang thử giữ lại phòng");
        }

        try {
            allocator.allocate(
                    booking.getRoomType().getId(),
                    booking.getCheckIn(),
                    booking.getCheckOut(),
                    booking.getRoomQuantity(),
                    picked -> {
                        bookingTx.reassignInNewTransaction(booking.getId(), picked);
                        return picked;
                    });
            log.info("Đơn {} đã giành lại được {} phòng sau khi tiền về muộn",
                    booking.getCode(), booking.getRoomQuantity());
            return true;
        } catch (RoomNotAvailable e) {
            // Chỉ nuốt đúng ngoại lệ này. Mọi lỗi khác phải nổi lên để sự kiện
            // webhook được ghi ERROR và xử lý lại được ở lần SePay gửi lại —
            // nuốt hết là quay về đúng cái "tiền vào mà không ai biết".
            log.warn("Đơn {} có tiền về muộn nhưng KHÔNG còn phòng — chuyển đối soát thủ công",
                    booking.getCode());
            return false;
        }
    }
}
