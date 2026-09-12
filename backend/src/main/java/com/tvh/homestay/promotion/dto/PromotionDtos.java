package com.tvh.homestay.promotion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO cho việc thử mã giảm giá trước khi đặt phòng. */
public final class PromotionDtos {

    private PromotionDtos() {}

    /**
     * Yêu cầu thử mã.
     *
     * <p>Không nhận số tiền từ client: giá lấy từ loại phòng trong cơ sở dữ
     * liệu, số đêm tính từ ngày. Nhận subtotal do client gửi là để client tự
     * khai mình đặt 100 triệu rồi nhận mã giảm 10% của con số đó.
     */
    public record CheckPromotionRequest(
            @NotBlank @Size(max = 50) String code,
            @NotNull Long roomTypeId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) int roomQuantity) {}

    /** Kết quả thử mã. Mọi con số ở đây do backend tính. */
    public record CheckPromotionResponse(
            String code,
            String name,
            String description,
            int nights,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal depositAmount) {}
}
