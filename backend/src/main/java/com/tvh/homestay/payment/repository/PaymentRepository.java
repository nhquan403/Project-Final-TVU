package com.tvh.homestay.payment.repository;

import com.tvh.homestay.payment.entity.Payment;
import com.tvh.homestay.payment.entity.ReconcileStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lần thanh toán; một đơn có thể có nhiều dòng. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingIdOrderByAttemptNoAsc(Long bookingId);

    Optional<Payment> findByTransferContent(String transferContent);

    /**
     * Hàng đợi đối soát: những khoản tiền cần người xử lý.
     *
     * <p>Điều kiện {@code reconcile_status <> 'NONE'} khớp đúng index từng phần
     * {@code idx_payments_reconcile} dựng ở V4, nên truy vấn này không quét bảng.
     */
    @Query("""
            select p from Payment p
            where p.reconcileStatus <> com.tvh.homestay.payment.entity.ReconcileStatus.NONE
              and (:status is not null or p.reconcileStatus <>
                   com.tvh.homestay.payment.entity.ReconcileStatus.RESOLVED)
              and (:status is null or p.reconcileStatus = :status)
            order by p.id desc
            """)
    Page<Payment> findReconcileQueue(@Param("status") ReconcileStatus status, Pageable pageable);

    /**
     * Số khoản CÒN CẦN người xử lý.
     *
     * <p>{@code RESOLVED} nằm ngoài phép đếm này, dù nó cũng khác {@code NONE}.
     * Đếm cả nó thì con số trên badge không bao giờ giảm — và một badge chỉ
     * tăng là một badge người dùng học cách không nhìn nữa, tức là hàng đợi lại
     * quay về chỗ không ai xử lý.
     */
    long countByReconcileStatusIn(java.util.Collection<ReconcileStatus> statuses);

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
