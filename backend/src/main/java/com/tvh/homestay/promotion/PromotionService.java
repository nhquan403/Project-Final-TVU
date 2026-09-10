package com.tvh.homestay.promotion;

import com.tvh.homestay.booking.exception.BookingExceptions.InvalidPromotion;
import com.tvh.homestay.booking.exception.BookingExceptions.PromotionExhausted;
import com.tvh.homestay.promotion.entity.Promotion;
import com.tvh.homestay.promotion.repository.PromotionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kiểm tra, tiêu thụ và hoàn lượt mã giảm giá. */
@Service
public class PromotionService {

    private final PromotionRepository promotions;
    private final Clock clock;

    public PromotionService(PromotionRepository promotions, Clock clock) {
        this.promotions = promotions;
        this.clock = clock;
    }

    /**
     * Tìm và kiểm mã. Trả {@code null} khi khách không nhập mã nào.
     *
     * <p>Chỉ kiểm điều kiện, KHÔNG tiêu thụ lượt: khách có thể xem thử mã nhiều
     * lần trước khi đặt, và mỗi lần xem mà đốt một lượt thì mã sẽ hết trước khi
     * ai kịp trả tiền.
     */
    @Transactional(readOnly = true)
    public Promotion validate(String code, int nights, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Promotion promotion = promotions.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new InvalidPromotion("Mã khuyến mãi không tồn tại."));

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!promotion.isActive()) {
            throw new InvalidPromotion("Mã khuyến mãi đã ngừng áp dụng.");
        }
        if (now.isBefore(promotion.getStartsAt()) || !now.isBefore(promotion.getEndsAt())) {
            throw new InvalidPromotion("Mã khuyến mãi không còn trong thời gian áp dụng.");
        }
        if (nights < promotion.getMinNights()) {
            throw new InvalidPromotion(
                    "Mã này yêu cầu đặt tối thiểu " + promotion.getMinNights() + " đêm.");
        }
        if (subtotal.compareTo(promotion.getMinTotalAmount()) < 0) {
            throw new InvalidPromotion("Đơn hàng chưa đạt giá trị tối thiểu của mã này.");
        }
        if (promotion.getUsageLimit() != null
                && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new PromotionExhausted();
        }
        return promotion;
    }

    /** Tiêu thụ một lượt. Ném khi mã vừa hết lượt do có người nhanh tay hơn. */
    @Transactional
    public void consume(Promotion promotion) {
        if (promotion == null) {
            return;
        }
        if (promotions.consumeOne(promotion.getId()) == 0) {
            throw new PromotionExhausted();
        }
    }

    /** Hoàn một lượt khi đơn vào trạng thái kết thúc. */
    @Transactional
    public void release(Promotion promotion) {
        if (promotion != null) {
            promotions.releaseOne(promotion.getId());
        }
    }
}
