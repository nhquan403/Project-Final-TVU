package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.ReviewView;
import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.review.entity.Review;
import com.tvh.homestay.review.entity.ReviewStatus;
import com.tvh.homestay.review.repository.ReviewRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Duyệt, từ chối và trả lời đánh giá.
 *
 * <p>Nội dung đánh giá do người ẩn danh gửi và được lưu dưới dạng VĂN BẢN THUẦN
 * (xem entity {@code Review}). API trả nguyên văn bản đó; màn hình duyệt hiển
 * thị bằng text binding chứ không {@code innerHTML} — đây đúng là nơi quản trị
 * viên bắt buộc phải mở nội dung lạ ra đọc, nên cũng là nơi một lỗ XSS lưu trữ
 * sẽ trúng đích.
 */
@Service
public class AdminReviewService {

    private final ReviewRepository reviews;
    private final Clock clock;

    public AdminReviewService(ReviewRepository reviews, Clock clock) {
        this.reviews = reviews;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ReviewView> list(String status) {
        return reviews.findWithBooking(parseStatus(status)).stream()
                .map(AdminReviewService::toView)
                .toList();
    }

    @Transactional
    public ReviewView changeStatus(Long id, ReviewStatus target) {
        Review review = require(id);
        review.setStatus(target);
        reviews.save(review);
        return toView(review);
    }

    @Transactional
    public ReviewView reply(Long id, String reply) {
        Review review = require(id);
        review.setAdminReply(reply.trim());
        review.setRepliedAt(OffsetDateTime.now(clock));
        reviews.save(review);
        return toView(review);
    }

    private Review require(Long id) {
        return reviews.findById(id).orElseThrow(() -> new AdminResourceNotFound("đánh giá #" + id));
    }

    private static ReviewStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ReviewStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidAdminRequest("Trạng thái đánh giá không hợp lệ: " + status);
        }
    }

    private static ReviewView toView(Review review) {
        return new ReviewView(
                review.getId(),
                review.getBooking().getCode(),
                review.getGuestNameSnapshot(),
                review.getRating(),
                review.getTitle(),
                review.getContent(),
                review.getStatus().name(),
                review.getAdminReply(),
                review.getRepliedAt(),
                review.getCreatedAt());
    }
}
