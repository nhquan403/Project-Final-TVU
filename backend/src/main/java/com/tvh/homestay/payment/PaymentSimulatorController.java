package com.tvh.homestay.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvh.homestay.payment.entity.Payment;
import com.tvh.homestay.payment.entity.WebhookProcessingResult;
import com.tvh.homestay.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Mô phỏng một lần chuyển khoản, để trình diễn khi không có tiền thật.
 *
 * <p><b>Ba rào chắn cùng lúc, không phải một.</b>
 *
 * <ol>
 *   <li>Đường dẫn nằm dưới {@code /api/admin/dev/**}, mà {@code SecurityConfig}
 *       đòi vai trò ADMIN.
 *   <li>{@link ConditionalOnProperty} — cờ {@code payments.simulator.enabled}
 *       MẶC ĐỊNH TẮT, kể cả ở profile {@code demo}. Không có cờ thì bean này
 *       không tồn tại, nên đường dẫn trả 404 chứ không phải 403.
 *   <li>{@code @Profile("!prod")} — không bao giờ nạp ở bản triển khai thật.
 * </ol>
 *
 * <p>Chỉ dùng {@code @Profile("dev")} như bản kế hoạch đầu là KHÔNG đủ, và sai
 * theo hướng nguy hiểm nhất: dự án này chỉ có hai profile, bản docker compose
 * chạy {@code demo}, nên endpoint sẽ có mặt ở đúng bản đem đi trình diễn — và
 * bất kỳ ai đặt phòng xong rồi gọi nó là tự xác nhận được đơn mà không trả đồng
 * nào.
 */
@RestController
@Profile("!prod")
@ConditionalOnProperty(prefix = "payments.simulator", name = "enabled", havingValue = "true")
public class PaymentSimulatorController {

    private static final DateTimeFormatter SEPAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SepayWebhookService webhooks;
    private final PaymentRepository payments;
    private final VietQrGenerator qr;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentSimulatorController(
            SepayWebhookService webhooks,
            PaymentRepository payments,
            VietQrGenerator qr,
            ObjectMapper objectMapper,
            Clock clock) {
        this.webhooks = webhooks;
        this.payments = payments;
        this.qr = qr;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Dựng một payload y hệt SePay rồi cho nó đi qua ĐÚNG đường xử lý thật.
     *
     * <p>Không gọi tắt vào tầng trong: nếu đường mô phỏng khác đường thật thì
     * buổi trình diễn chứng minh được một thứ mà bản chạy thật không có.
     */
    @PostMapping("/api/admin/dev/payments/{transferContent}/simulate-transfer")
    public ResponseEntity<Map<String, Object>> simulate(
            @PathVariable String transferContent,
            @RequestParam(name = "amount", required = false) BigDecimal amount,
            @RequestParam(name = "transferType", defaultValue = "in") String transferType) {

        Payment payment = payments.findByTransferContent(transferContent)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không có lần thanh toán nào mang nội dung " + transferContent));

        BigDecimal transferred = amount != null ? amount : payment.getAmountExpected();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", ThreadLocalRandom.current().nextLong(100_000, 999_999_999));
        payload.put("gateway", "SimulatorBank");
        payload.put("transactionDate", LocalDateTime.now(clock).format(SEPAY_TIME));
        payload.put("accountNumber", qr.getAccountNumber());
        payload.put("code", null);
        payload.put("content", transferContent);
        payload.put("transferType", transferType);
        payload.put("description", "Giao dich mo phong " + transferContent);
        payload.put("transferAmount", transferred);
        payload.put("accumulated", 0);
        payload.put("referenceCode", "SIM" + System.nanoTime());

        WebhookProcessingResult result = webhooks.handle(writeJson(payload));
        return ResponseEntity.ok(Map.of(
                "transferContent", transferContent,
                "transferAmount", transferred,
                "result", result.name()));
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Không dựng được payload mô phỏng", e);
        }
    }
}
