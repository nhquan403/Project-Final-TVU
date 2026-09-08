package com.tvh.homestay.payment.repository;

import com.tvh.homestay.payment.entity.OutboundEmail;
import org.springframework.data.jpa.repository.JpaRepository;

/** Hộp thư đi — email xếp hàng chờ gửi. */
public interface OutboundEmailRepository extends JpaRepository<OutboundEmail, Long> {
}
