package com.tvh.homestay.payment.repository;

import com.tvh.homestay.payment.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lần thanh toán; một đơn có thể có nhiều dòng. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingIdOrderByAttemptNoAsc(Long bookingId);

    Optional<Payment> findByTransferContent(String transferContent);

    /**
     * Đọc lần thanh toán và GIỮ KHOÁ tới cuối transaction.
     *
     * <p>{@code amount_received} là số CỘNG DỒN. Hai webhook về gần nhau mà
     * không có khoá này thì cả hai cùng đọc số cũ, cùng cộng phần của mình, và
     * lần ghi sau nuốt mất lần ghi trước — khách chuyển đủ tiền qua hai lần
     * nhưng hệ thống chỉ thấy một lần.
     *
     * <p>Lý do dùng {@code FOR NO KEY UPDATE}: xem {@code BookingRepository#lockById}.
     */
    @Query(value = "SELECT * FROM payments WHERE transfer_content = :content FOR NO KEY UPDATE",
            nativeQuery = true)
    Optional<Payment> lockByTransferContent(@Param("content") String content);
}
