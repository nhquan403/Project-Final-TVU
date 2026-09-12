package com.tvh.homestay.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/** DTO đánh giá của khách. */
public final class ReviewDtos {

    private ReviewDtos() {}

    /**
     * Nội dung khách gửi.
     *
     * <p>KHÔNG có trường tên người gửi: tên được chụp từ chính đơn đặt phòng.
     * Nhận tên từ client nghĩa là ai gửi được đánh giá cũng ký tên bất kỳ ai.
     *
     * <p>{@code token} hoặc {@code phone} — cùng mức xác thực với tra cứu và
     * huỷ đơn. Xem javadoc {@code ReviewService.submit}.
     */
    public record SubmitReviewRequest(
            @Size(max = 32) String token,
            @Size(max = 20) String phone,
            @Min(1) @Max(5) short rating,
            @Size(max = 200) String title,
            @Size(max = 4000) String content) {}

    /** Đánh giá hiển thị công khai. Văn bản THUẦN — giao diện dùng text binding. */
    public record PublicReview(
            Long id,
            String guestName,
            short rating,
            String title,
            String content,
            String roomTypeName,
            String adminReply,
            OffsetDateTime createdAt) {}
}
