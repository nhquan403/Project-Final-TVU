package com.tvh.homestay.payment.repository;

import com.tvh.homestay.payment.entity.OutboundEmail;
import com.tvh.homestay.payment.entity.OutboundEmailStatus;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/** Hộp thư đi — email xếp hàng chờ gửi. */
public interface OutboundEmailRepository extends JpaRepository<OutboundEmail, Long> {

    /** Lô thư kế tiếp cho bộ gửi, cũ trước mới sau. */
    List<OutboundEmail> findByStatusOrderByIdAsc(OutboundEmailStatus status, Limit limit);

    long countByBookingIdAndTemplate(Long bookingId, String template);

    List<OutboundEmail> findByBookingIdOrderByIdAsc(Long bookingId);
}
