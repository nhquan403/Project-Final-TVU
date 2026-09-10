package com.tvh.homestay.payment.repository;

import com.tvh.homestay.payment.entity.PaymentWebhookEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Nhật ký webhook và khoá chống xử lý trùng. */
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    /**
     * Tra theo khoá chống trùng {@code (provider, external_id)}.
     *
     * <p>Kết quả CHƯA đủ để quyết định bỏ qua: một bản ghi cũ có
     * {@code processing_result = ERROR} nghĩa là lần trước hỏng giữa chừng và
     * lần này phải xử lý lại. Xem {@code SepayWebhookTxService.claim}.
     */
    Optional<PaymentWebhookEvent> findByProviderAndExternalId(String provider, String externalId);
}
