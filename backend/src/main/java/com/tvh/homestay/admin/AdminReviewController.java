package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.ReviewReplyRequest;
import com.tvh.homestay.admin.dto.AdminDtos.ReviewView;
import com.tvh.homestay.review.entity.ReviewStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Duyệt đánh giá của khách. */
@RestController
public class AdminReviewController {

    private final AdminReviewService reviews;

    public AdminReviewController(AdminReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/api/admin/reviews")
    public List<ReviewView> list(@RequestParam(required = false) String status) {
        return reviews.list(status);
    }

    @PostMapping("/api/admin/reviews/{id}/approve")
    public ReviewView approve(@PathVariable Long id) {
        return reviews.changeStatus(id, ReviewStatus.APPROVED);
    }

    @PostMapping("/api/admin/reviews/{id}/reject")
    public ReviewView reject(@PathVariable Long id) {
        return reviews.changeStatus(id, ReviewStatus.REJECTED);
    }

    @PostMapping("/api/admin/reviews/{id}/reply")
    public ReviewView reply(@PathVariable Long id, @Valid @RequestBody ReviewReplyRequest request) {
        return reviews.reply(id, request.reply());
    }
}
