package com.tvh.homestay.booking;

import com.tvh.homestay.promotion.entity.DiscountType;
import com.tvh.homestay.promotion.entity.Promotion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Tính tiền cho một đơn.
 *
 * <p><b>Mọi con số ở đây do backend quyết định.</b> Frontend chỉ hiển thị. Nhận
 * số tiền từ client rồi tin là mở đường cho việc sửa giá ngay trên trình duyệt.
 */
@Service
public class BookingPricingService {

    /** Làm tròn tiền cọc tới bội số này — khách chuyển khoản số tròn dễ hơn. */
    private static final BigDecimal DEPOSIT_ROUNDING_UNIT = BigDecimal.valueOf(1000);

    private final BigDecimal depositRate;

    public BookingPricingService(@Value("${booking.deposit-rate:0.30}") BigDecimal depositRate) {
        this.depositRate = depositRate;
    }

    /** Kết quả tính tiền của một đơn. */
    public record Quote(
            BigDecimal unitPrice,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            BigDecimal deposit) {}

    public Quote quote(BigDecimal pricePerNight, int nights, int roomQuantity, Promotion promotion) {
        BigDecimal subtotal = pricePerNight
                .multiply(BigDecimal.valueOf((long) nights * roomQuantity))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = discountFor(promotion, subtotal);
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal deposit = roundToUnit(total.multiply(depositRate));
        return new Quote(pricePerNight, subtotal, discount, total, deposit);
    }

    private static BigDecimal discountFor(Promotion promotion, BigDecimal subtotal) {
        if (promotion == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (promotion.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal
                    .multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promotion.getMaxDiscountAmount() != null) {
                discount = discount.min(promotion.getMaxDiscountAmount());
            }
        } else {
            discount = promotion.getDiscountValue();
        }
        // Giảm giá không bao giờ vượt quá tiền hàng: một mã FIXED lớn hơn đơn
        // hàng mà không chặn ở đây sẽ cho ra tổng tiền âm.
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal roundToUnit(BigDecimal amount) {
        return amount
                .divide(DEPOSIT_ROUNDING_UNIT, 0, RoundingMode.HALF_UP)
                .multiply(DEPOSIT_ROUNDING_UNIT)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
