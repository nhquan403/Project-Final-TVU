package com.tvh.homestay.review.repository;

import com.tvh.homestay.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

/** Đánh giá của khách, chờ admin duyệt mới hiện. */
public interface ReviewRepository extends JpaRepository<Review, Long> {
}
