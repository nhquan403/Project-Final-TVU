package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.BookingRoom;
import com.tvh.homestay.booking.entity.BookingRoomStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Gán phòng vật lý cho đơn, trong một khoảng ngày. */
public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {

    List<BookingRoom> findByBookingId(Long bookingId);

    /**
     * Lấy kèm phòng vật lý trong MỘT truy vấn.
     *
     * <p>{@code join fetch} ở đây là bắt buộc chứ không phải tối ưu: nơi gọi
     * dựng phản hồi ở NGOÀI transaction — vòng thử gán phòng không thể
     * transactional — và {@code open-in-view} đã tắt. Để {@code room} ở dạng
     * proxy lười thì lúc đọc số phòng sẽ ném {@code LazyInitializationException}.
     */
    @Query("select br from BookingRoom br join fetch br.room"
            + " where br.booking.id = :bookingId and br.status = :status order by br.id")
    List<BookingRoom> findWithRoomByBookingIdAndStatus(
            @Param("bookingId") Long bookingId, @Param("status") BookingRoomStatus status);

    List<BookingRoom> findByBookingIdAndStatus(Long bookingId, BookingRoomStatus status);

    /**
     * Nhả phòng của một đơn.
     *
     * <p>Ràng buộc chống trùng chỉ soi các dòng ACTIVE, nên đổi trạng thái sang
     * RELEASED là mở lại slot NGAY, trong khi lịch sử vẫn còn để tra cứu.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BookingRoom br"
            + " set br.status = com.tvh.homestay.booking.entity.BookingRoomStatus.RELEASED"
            + " where br.booking.id in :bookingIds"
            + " and br.status = com.tvh.homestay.booking.entity.BookingRoomStatus.ACTIVE")
    int releaseByBookingIds(@Param("bookingIds") List<Long> bookingIds);
}
