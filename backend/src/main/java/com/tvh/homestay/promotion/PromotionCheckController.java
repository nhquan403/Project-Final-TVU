package com.tvh.homestay.promotion;

import com.tvh.homestay.availability.AvailabilityService;
import com.tvh.homestay.booking.BookingPricingService;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidBookingRequest;
import com.tvh.homestay.promotion.dto.PromotionDtos.CheckPromotionRequest;
import com.tvh.homestay.promotion.dto.PromotionDtos.CheckPromotionResponse;
import com.tvh.homestay.promotion.entity.Promotion;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.repository.RoomTypeRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thử một mã giảm giá và cho biết NGAY nó giảm bao nhiêu.
 *
 * <p>Đường dẫn này đã được khai {@code permitAll} và đã có dòng hạn mức tần
 * suất riêng ({@code ip:promo}, 20 lượt/phút) từ Phase 4, nhưng tới Phase 8 mới
 * có lớp cài đặt.
 *
 * <p>Hạn mức đó không phải trang trí: không có nó, endpoint này là một máy dò
 * mã giảm giá — gọi vài nghìn lần là tìm ra mọi mã đang chạy.
 *
 * <p>Tính tiền bằng ĐÚNG {@link BookingPricingService} mà luồng đặt phòng dùng.
 * Viết lại công thức ở đây là tạo ra hai công thức, và chúng sẽ lệch nhau đúng
 * lúc khách nhìn thấy cả hai con số.
 */
@RestController
public class PromotionCheckController {

    private final PromotionService promotions;
    private final BookingPricingService pricing;
    private final AvailabilityService availability;
    private final RoomTypeRepository roomTypes;

    public PromotionCheckController(
            PromotionService promotions,
            BookingPricingService pricing,
            AvailabilityService availability,
            RoomTypeRepository roomTypes) {
        this.promotions = promotions;
        this.pricing = pricing;
        this.availability = availability;
        this.roomTypes = roomTypes;
    }

    @PostMapping("/api/promotions/check")
    public CheckPromotionResponse check(@Valid @RequestBody CheckPromotionRequest request) {
        availability.validateDates(request.checkIn(), request.checkOut());
        RoomType roomType = roomTypes.findById(request.roomTypeId())
                .orElseThrow(() -> new InvalidBookingRequest("Không tìm thấy loại phòng."));

        int nights = AvailabilityService.nights(request.checkIn(), request.checkOut());
        BigDecimal subtotalBeforePromotion = roomType.getBasePrice()
                .multiply(BigDecimal.valueOf((long) nights * request.roomQuantity()));

        // validate() ném INVALID_PROMOTION (400) hoặc PROMOTION_EXHAUSTED (409);
        // ApiExceptionHandler đã đổi chúng thành problem+json kèm mã ổn định.
        Promotion promotion =
                promotions.validate(request.code(), nights, subtotalBeforePromotion);
        BookingPricingService.Quote quote = pricing.quote(
                roomType.getBasePrice(), nights, request.roomQuantity(), promotion);

        return new CheckPromotionResponse(
                promotion.getCode(),
                promotion.getName(),
                promotion.getDescription(),
                nights,
                quote.subtotal(),
                quote.discount(),
                quote.total(),
                quote.deposit());
    }
}
