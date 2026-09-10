package com.tvh.homestay.payment;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điểm nhận webhook của SePay.
 *
 * <p><b>Phản hồi phải là HTTP 200 KÈM thân {@code {"success": true}}.</b> SePay
 * chỉ coi là giao thành công khi thấy đúng thân đó; trả 200 với thân rỗng vẫn
 * bị đưa vào hàng đợi gửi lại tới 7 lần trong 5 giờ. Vì vậy mọi kết quả xử lý —
 * kể cả không khớp đơn nào — đều trả 200: khoản tiền đã được ghi vào
 * {@code payment_webhook_events}, gửi lại cũng không đổi kết quả.
 *
 * <p>Ngoại lệ duy nhất là sai khoá: 401, và không ghi gì cả.
 *
 * <p>Nhận thân request dưới dạng {@code String} chứ không phải DTO là chủ ý:
 * nguyên văn payload phải được lưu lại làm chứng cứ đối soát, và một thân
 * request lệch schema không được phép biến thành 400 trước khi kịp ghi lại.
 */
@RestController
public class SepayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SepayWebhookController.class);

    private static final String SCHEME = "Apikey ";
    private static final Map<String, Object> SUCCESS = Map.of("success", true);

    private final SepayWebhookService webhooks;
    private final String apiKey;

    public SepayWebhookController(
            SepayWebhookService webhooks, @Value("${SEPAY_WEBHOOK_API_KEY}") String apiKey) {
        this.webhooks = webhooks;
        this.apiKey = apiKey;
    }

    @PostMapping("/api/payments/webhook/sepay")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) String rawBody) {

        if (!authorized(authorization)) {
            // Không log giá trị khoá — chỉ ghi rằng có một lần gọi sai.
            log.warn("Webhook SePay bị từ chối: thiếu hoặc sai khoá xác thực");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false));
        }

        webhooks.handle(rawBody == null ? "" : rawBody);
        return ResponseEntity.ok(SUCCESS);
    }

    private boolean authorized(String authorization) {
        if (authorization == null || !authorization.startsWith(SCHEME)) {
            return false;
        }
        return SepayWebhookService.apiKeyMatches(
                authorization.substring(SCHEME.length()).trim(), apiKey);
    }
}
