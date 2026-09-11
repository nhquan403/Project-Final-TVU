package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.PromotionRequest;
import com.tvh.homestay.admin.dto.AdminDtos.PromotionView;
import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.admin.exception.AdminExceptions.ResourceInUse;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.promotion.entity.DiscountType;
import com.tvh.homestay.promotion.entity.Promotion;
import com.tvh.homestay.promotion.repository.PromotionRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD mã khuyến mãi. */
@Service
public class AdminPromotionService {

    private final PromotionRepository promotions;
    private final BookingRepository bookings;

    public AdminPromotionService(PromotionRepository promotions, BookingRepository bookings) {
        this.promotions = promotions;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<PromotionView> list() {
        return promotions.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(this::toView)
                .toList();
    }

    @Transactional
    public PromotionView create(PromotionRequest request) {
        validate(request);
        promotions.findByCodeIgnoreCase(request.code()).ifPresent(existing -> {
            throw new InvalidAdminRequest("Mã " + request.code() + " đã tồn tại.");
        });
        Promotion promotion = new Promotion();
        apply(promotion, request, true);
        promotions.save(promotion);
        return toView(promotion);
    }

    /**
     * Sửa mã khuyến mãi.
     *
     * <p>Trường {@code code} bị KHOÁ khi đã có đơn dùng mã: nó nằm trong dữ liệu
     * đối soát và trong thư đã gửi cho khách. Đổi nó là làm cho những bản ghi đó
     * nói về một mã không còn tồn tại.
     */
    @Transactional
    public PromotionView update(Long id, PromotionRequest request) {
        validate(request);
        Promotion promotion = require(id);
        long used = bookings.countByPromotionId(id);
        boolean codeChanged = !promotion.getCode().equalsIgnoreCase(request.code());
        if (codeChanged && used > 0) {
            throw new ResourceInUse(
                    "Đã có " + used + " đơn dùng mã này nên không đổi được mã. "
                            + "Hãy tắt mã hiện tại và tạo mã mới nếu cần.");
        }
        apply(promotion, request, false);
        promotions.save(promotion);
        return toView(promotion);
    }

    @Transactional
    public void delete(Long id) {
        Promotion promotion = require(id);
        long used = bookings.countByPromotionId(id);
        if (used > 0) {
            throw new ResourceInUse(
                    "Đã có " + used + " đơn dùng mã này nên không xoá được. Hãy tắt mã thay vì xoá.");
        }
        promotions.delete(promotion);
    }

    // ─────────────────────────────────────────────────────────────────────

    private Promotion require(Long id) {
        return promotions.findById(id)
                .orElseThrow(() -> new AdminResourceNotFound("mã khuyến mãi #" + id));
    }

    private static void validate(PromotionRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new InvalidAdminRequest("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        DiscountType type = parseType(request.discountType());
        if (type == DiscountType.PERCENT
                && request.discountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new InvalidAdminRequest("Giảm theo phần trăm không vượt quá 100.");
        }
        if (request.usageLimit() != null && request.usageLimit() < 1) {
            throw new InvalidAdminRequest("Giới hạn lượt dùng phải từ 1 trở lên, hoặc để trống.");
        }
    }

    private static DiscountType parseType(String type) {
        try {
            return DiscountType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new InvalidAdminRequest("Kiểu giảm giá không hợp lệ: " + type);
        }
    }

    private static void apply(Promotion promotion, PromotionRequest request, boolean setCode) {
        if (setCode) {
            promotion.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        }
        promotion.setName(request.name().trim());
        promotion.setDescription(request.description());
        promotion.setDiscountType(parseType(request.discountType()));
        promotion.setDiscountValue(request.discountValue());
        promotion.setMaxDiscountAmount(request.maxDiscountAmount());
        promotion.setMinNights(request.minNights());
        promotion.setMinTotalAmount(request.minTotalAmount());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());
        promotion.setUsageLimit(request.usageLimit());
        promotion.setActive(request.active());
    }

    private PromotionView toView(Promotion promotion) {
        return new PromotionView(
                promotion.getId(),
                promotion.getCode(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getDiscountType().name(),
                promotion.getDiscountValue(),
                promotion.getMaxDiscountAmount(),
                promotion.getMinNights(),
                promotion.getMinTotalAmount(),
                promotion.getStartsAt(),
                promotion.getEndsAt(),
                // NULL đi thẳng ra API. Giao diện hiển thị "không giới hạn" —
                // đổi nó thành 0 hay -1 ở đây là bắt phía kia đoán ý.
                promotion.getUsageLimit(),
                promotion.getUsedCount(),
                promotion.isActive(),
                bookings.countByPromotionId(promotion.getId()));
    }
}
