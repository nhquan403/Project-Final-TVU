package com.tvh.homestay.review.repository;

import com.tvh.homestay.review.entity.Review;
import com.tvh.homestay.review.entity.ReviewStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Đánh giá của khách sau khi trả phòng. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Nạp kèm đơn để lấy mã đơn mà không phải n+1 truy vấn.
     *
     * <p>{@code :status is null} cho phép một truy vấn phục vụ cả "tất cả" lẫn
     * từng trạng thái.
     */
    @Query("""
            select r from Review r
            join fetch r.booking
            where (:status is null or r.status = :status)
            order by r.id desc
            """)
    List<Review> findWithBooking(@Param("status") ReviewStatus status);

    /**
     * Đánh giá hiện trên trang công khai: CHỈ những đánh giá đã được duyệt.
     *
     * <p>Đây là nửa còn lại của cơ chế kiểm duyệt. Thiếu điều kiện này thì màn
     * hình duyệt của admin chỉ là trang trí — nội dung của người ẩn danh ra
     * thẳng trang chủ ngay khi gửi.
     */
    List<Review> findByStatusOrderByIdDesc(ReviewStatus status, Limit limit);

    boolean existsByBookingId(Long bookingId);

    Optional<Review> findByBookingId(Long bookingId);
}
