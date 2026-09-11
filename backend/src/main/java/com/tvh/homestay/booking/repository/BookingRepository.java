package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Đơn đặt phòng. */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByCode(String code);

    Optional<Booking> findByCode(String code);

    /** Danh sách đơn của một tài khoản. Lọc theo id lấy từ token, không tin client. */
    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Booking> findByCodeAndUserId(String code, Long userId);

    /**
     * Danh sách đơn cho khu quản trị, lọc và phân trang PHÍA MÁY CHỦ.
     *
     * <p>Mỗi điều kiện đều có dạng {@code :x is null or ...} để một truy vấn
     * phục vụ mọi tổ hợp bộ lọc. Tải hết đơn về rồi lọc trong Java là cách
     * chạy được ở bản demo vài chục đơn và sập ở bản thật.
     *
     * <p>Không {@code join fetch} loại phòng: tên loại phòng đã được chụp lại
     * vào {@code roomTypeNameSnapshot} ngay lúc đặt, nên bảng danh sách không
     * cần chạm tới bảng kia — và {@code join fetch} đi cùng phân trang sẽ khiến
     * Hibernate phân trang trong bộ nhớ.
     */
    @Query("""
            select b from Booking b
            where (:status is null or b.status = :status)
              and (:from is null or b.checkIn >= :from)
              and (:to is null or b.checkIn <= :to)
              and (:q is null
                   or lower(b.code) like :q
                   or lower(b.guestName) like :q
                   or lower(b.guestPhone) like :q)
            """)
    Page<Booking> search(
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            Pageable pageable);

    /** Cùng bộ lọc với {@link #search}, không phân trang — dùng để xuất CSV. */
    @Query("""
            select b from Booking b
            where (:status is null or b.status = :status)
              and (:from is null or b.checkIn >= :from)
              and (:to is null or b.checkIn <= :to)
            order by b.checkIn, b.id
            """)
    List<Booking> findForExport(
            @Param("status") BookingStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Đơn còn hiệu lực đang giữ một phòng cụ thể, từ hôm nay trở đi. */
    @Query("""
            select b from Booking b
            join BookingRoom br on br.booking = b
            where br.room.id = :roomId
              and br.status = com.tvh.homestay.booking.entity.BookingRoomStatus.ACTIVE
              and b.checkOut > :today
              and b.status in (com.tvh.homestay.booking.entity.BookingStatus.PENDING_PAYMENT,
                               com.tvh.homestay.booking.entity.BookingStatus.CONFIRMED,
                               com.tvh.homestay.booking.entity.BookingStatus.AWAITING_REVIEW,
                               com.tvh.homestay.booking.entity.BookingStatus.CHECKED_IN)
            order by b.checkIn
            """)
    List<Booking> findActiveFutureByRoom(@Param("roomId") Long roomId, @Param("today") LocalDate today);

    long countByRoomTypeId(Long roomTypeId);

    long countByPromotionId(Long promotionId);

    /**
     * Đọc đơn và GIỮ KHOÁ tới cuối transaction, dùng khi webhook ghi nhận tiền.
     *
     * <p>Không có khoá này thì bộ quét hết hạn và webhook chạy song song sẽ
     * cùng đọc trạng thái {@code PENDING_PAYMENT}, bộ quét ghi {@code EXPIRED}
     * rồi webhook ghi đè {@code CONFIRMED} lên trên — đơn thành CONFIRMED
     * nhưng phòng đã bị nhả, và không có gì trong dữ liệu cho thấy điều đó.
     *
     * <p><b>{@code FOR NO KEY UPDATE} chứ không phải {@code FOR UPDATE}.</b>
     * Mỗi lần chèn một dòng {@code booking_rooms} đều lấy khoá {@code FOR KEY
     * SHARE} trên đúng dòng {@code bookings} này vì có khoá ngoại. {@code FOR
     * UPDATE} xung khắc với {@code FOR KEY SHARE}, nên nhánh gán lại phòng —
     * chạy ở transaction riêng trong khi transaction này còn giữ khoá — sẽ
     * treo tới khi hết {@code lock_timeout}. {@code FOR NO KEY UPDATE} vẫn
     * xung khắc với chính nó và với UPDATE thường (tức là vẫn xếp hàng webhook
     * với bộ quét), nhưng không chặn khoá ngoại.
     */
    @Query(value = "SELECT * FROM bookings WHERE id = :id FOR NO KEY UPDATE", nativeQuery = true)
    Optional<Booking> lockById(@Param("id") Long id);
}
