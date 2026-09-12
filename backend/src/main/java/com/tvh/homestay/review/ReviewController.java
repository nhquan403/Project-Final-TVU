package com.tvh.homestay.review;

import com.tvh.homestay.review.dto.ReviewDtos.PublicReview;
import com.tvh.homestay.review.dto.ReviewDtos.SubmitReviewRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đánh giá của khách.
 *
 * <p>Công khai ở tầng phân quyền, nhưng quyền trên TỪNG ĐƠN được kiểm ở tầng
 * service bằng {@code accessToken} hoặc số điện thoại — cùng cách mà tra cứu và
 * huỷ đơn đang dùng, và là cách duy nhất phục vụ được khách vãng lai không có
 * tài khoản.
 */
@RestController
public class ReviewController {

    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping("/api/bookings/{code}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicReview submit(
            @PathVariable String code, @Valid @RequestBody SubmitReviewRequest request) {
        return reviews.submit(code, request);
    }

    /** Chỉ đánh giá ĐÃ DUYỆT. Lọc ở truy vấn, không lọc ở frontend. */
    @GetMapping("/api/reviews")
    public List<PublicReview> published() {
        return reviews.published();
    }
}
