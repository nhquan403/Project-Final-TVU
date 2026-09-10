package com.tvh.homestay.payment.repository;

import com.tvh.homestay.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lần thanh toán; một đơn có thể có nhiều dòng. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingIdOrderByAttemptNoAsc(Long bookingId);

    Optional<Payment> findByTransferContent(String transferContent);
}
