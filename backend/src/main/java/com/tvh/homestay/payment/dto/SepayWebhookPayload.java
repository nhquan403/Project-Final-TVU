package com.tvh.homestay.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * Thân webhook do SePay gửi tới.
 *
 * <p>{@code ignoreUnknown = true} là bắt buộc: nhà cung cấp thêm trường mới mà
 * không báo trước, và một webhook bị từ chối vì trường lạ nghĩa là tiền đã vào
 * tài khoản nhưng hệ thống không biết. Bù lại, nguyên văn payload được ghi vào
 * {@code payment_webhook_events.payload}, nên trường bị bỏ qua hôm nay vẫn đọc
 * lại được khi cần đối soát.
 *
 * <p>Không trường nào ở đây được tin: số tiền, chiều tiền và số tài khoản đều
 * đi qua năm lớp kiểm tra ở {@code SepayWebhookService} trước khi ảnh hưởng tới
 * một đồng nào trong dữ liệu.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SepayWebhookPayload(
        Long id,
        String gateway,
        String transactionDate,
        String accountNumber,
        String subAccount,
        String code,
        String content,
        String transferType,
        String description,
        BigDecimal transferAmount,
        BigDecimal accumulated,
        String referenceCode) {

    /**
     * Khoá chống xử lý trùng phía nhà cung cấp.
     *
     * <p>Ưu tiên {@code id} vì đó là khoá chính của giao dịch bên SePay.
     * {@code referenceCode} chỉ là phương án dự phòng — nó KHÔNG duy nhất
     * tuyệt đối, nên nó là khoá dự phòng chứ không phải khoá chính.
     */
    public String externalId() {
        if (id != null) {
            return id.toString();
        }
        return referenceCode == null || referenceCode.isBlank() ? null : referenceCode.trim();
    }

    /** Chiều tiền, chuẩn hoá về chữ thường. Chỉ {@code "in"} mới là tiền VÀO. */
    public String normalizedTransferType() {
        return transferType == null ? "" : transferType.trim().toLowerCase();
    }

    public BigDecimal amountOrZero() {
        return transferAmount == null ? BigDecimal.ZERO : transferAmount;
    }
}
