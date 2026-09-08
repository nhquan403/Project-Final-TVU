package com.tvh.homestay.payment.repository;

import com.tvh.homestay.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Nhật ký webhook và khoá chống xử lý trùng. */
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
}
