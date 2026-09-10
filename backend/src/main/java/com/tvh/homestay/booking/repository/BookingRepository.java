package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.Booking;
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
