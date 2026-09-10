package com.tvh.homestay.mail;

import com.tvh.homestay.payment.entity.OutboundEmail;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lấy thư khỏi hộp thư đi và gửi đi, tách hẳn khỏi transaction nghiệp vụ.
 *
 * <p><b>Gửi NGOÀI transaction là chủ ý.</b> Một cuộc gọi SMTP có thể mất hàng
 * chục giây; giữ transaction cơ sở dữ liệu suốt thời gian đó là giữ luôn một
 * kết nối trong pool và một khoá trên dòng thư. Đổi lại, ngữ nghĩa ở đây là
 * <i>ít nhất một lần</i>: nếu tiến trình chết đúng giữa lúc SMTP đã nhận và
 * dòng {@code SENT} chưa kịp ghi, lá thư sẽ được gửi lại. Gửi trùng một thư xác
 * nhận là phiền; mất hẳn một thư xác nhận là mất khách.
 */
@Component
public class EmailDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchScheduler.class);

    private final EmailOutboxService outbox;
    private final MailService mail;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;

    public EmailDispatchScheduler(
            EmailOutboxService outbox,
            MailService mail,
            Clock clock,
            @Value("${mail.dispatch-batch-size:20}") int batchSize,
            @Value("${mail.max-attempts:5}") int maxAttempts) {
        this.outbox = outbox;
        this.mail = mail;
        this.clock = clock;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${mail.dispatch-ms:30000}")
    public void dispatchPending() {
        int sent = runOnce();
        if (sent > 0) {
            log.info("Đã gửi {} thư từ hộp thư đi qua {}", sent, mail.describeSender());
        }
    }

    /** Tách khỏi phương thức {@code @Scheduled} để test gọi thẳng được. */
    public int runOnce() {
        List<OutboundEmail> batch = outbox.nextPending(batchSize);
        int sent = 0;
        for (OutboundEmail email : batch) {
            Long id = email.getId();
            Map<String, Object> payload = outbox.readPayload(email).orElse(null);
            if (payload == null) {
                // Nội dung hỏng thì thử lại bao nhiêu lần cũng hỏng. Đẩy thẳng
                // tới số lần tối đa để nó rơi vào FAILED và hiện ra cho người
                // xử lý, thay vì quay vòng mãi trong hàng đợi.
                outbox.markFailure(id, "Nội dung thư trong hộp thư đi không đọc được", 1);
                continue;
            }
            try {
                mail.send(email.getTemplate(), email.getToEmail(), payload);
                outbox.markSent(id, OffsetDateTime.now(clock));
                sent++;
            } catch (RuntimeException e) {
                log.warn("Gửi thư {} (mẫu {}) hỏng: {}", id, email.getTemplate(), e.getMessage());
                outbox.markFailure(id, e.getMessage(), maxAttempts);
            }
        }
        return sent;
    }
}
