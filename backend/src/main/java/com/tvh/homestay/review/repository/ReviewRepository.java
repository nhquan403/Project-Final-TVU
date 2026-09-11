package com.tvh.homestay.review.repository;

import com.tvh.homestay.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

/** Đánh giá của khách, chờ admin duyệt mới hiện. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Nạp kèm đơn để lấy mã đơn mà không phải n+1 truy vấn.
     *
     * <p>{@code :status is null} cho phép một truy vấn phục vụ cả "tất cả" lẫn
     * từng trạng thái.
     */
    @org.springframework.data.jpa.repository.Query("""
            select r from Review r
            join fetch r.booking
            where (:status is null or r.status = :status)
            order by r.id desc
            """)
    java.util.List<Review> findWithBooking(
            @org.springframework.data.repository.query.Param("status")
            com.tvh.homestay.review.entity.ReviewStatus status);
}
