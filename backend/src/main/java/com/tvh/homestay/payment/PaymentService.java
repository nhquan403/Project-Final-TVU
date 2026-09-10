package com.tvh.homestay.payment;

import com.tvh.homestay.booking.BookingCodeGenerator;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.payment.entity.Payment;
import com.tvh.homestay.payment.entity.PaymentProvider;
import com.tvh.homestay.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tạo và tra cứu lần thanh toán của một đơn.
 *
 * <p>Một đơn có thể có NHIỀU lần thanh toán: khách chuyển hai lần, chuyển thiếu
 * rồi bù, hoặc đặt lại sau khi hết hạn. Nội dung chuyển khoản mang hậu tố số
 * thứ tự lần thử ({@code TVH8F3K2Q01}), nhờ đó QR của lần trước không khớp nhầm
 * vào lần sau — không có hậu tố này, khách quét lại ảnh QR cũ còn mở trong tab
 * sẽ trả tiền vào một đơn đã chết.
 */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final VietQrGenerator qr;

    public PaymentService(PaymentRepository payments, VietQrGenerator qr) {
        this.payments = payments;
        this.qr = qr;
    }

    /**
     * Tạo lần thanh toán đầu tiên cho một đơn vừa được ghi.
     *
     * <p>Gọi trong CÙNG transaction tạo đơn: đơn có mà không có dòng thanh toán
     * nghĩa là khách nhìn thấy màn hình QR trống và không có gì để đối soát.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Payment createForBooking(Booking booking) {
        String transferContent = BookingCodeGenerator.transferContent(booking.getCode(), 1);
        BigDecimal amount = booking.getDepositAmount();

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAttemptNo(1);
        payment.setProvider(PaymentProvider.SEPAY);
        payment.setAmountExpected(amount);
        payment.setTransferContent(transferContent);
        payment.setQrContent(qr.plainInstruction(amount, transferContent));
        payment.setQrImageUrl(qr.imageUrl(amount, transferContent));
        payment.setExpiresAt(booking.getHoldExpiresAt());
        return payments.save(payment);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> latestFor(Long bookingId) {
        List<Payment> attempts = payments.findByBookingIdOrderByAttemptNoAsc(bookingId);
        return attempts.isEmpty() ? Optional.empty() : Optional.of(attempts.get(attempts.size() - 1));
    }

    /** Gia hạn hạn thanh toán khi đơn được gia hạn giữ chỗ. */
    @Transactional
    public void extendExpiry(Payment payment, OffsetDateTime newExpiry) {
        payment.setExpiresAt(newExpiry);
        payments.save(payment);
    }
}
