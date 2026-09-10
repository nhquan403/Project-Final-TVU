package com.tvh.homestay.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvh.homestay.payment.dto.SepayWebhookPayload;
import com.tvh.homestay.payment.entity.WebhookProcessingResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Điều phối việc xử lý một webhook SePay.
 *
 * <p>Cố ý KHÔNG có {@code @Transactional}: lớp này ghép hai transaction có vòng
 * đời khác nhau — {@code claim} phải commit độc lập để dấu vết còn lại kể cả
 * khi bước sau hỏng, còn {@code process} phải rollback trọn vẹn khi hỏng. Gộp
 * cả hai vào một transaction thì một lỗi ở bước sau xoá luôn bằng chứng đã nhận
 * được tiền.
 */
@Service
public class SepayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(SepayWebhookService.class);

    private final SepayWebhookTxService tx;
    private final ObjectMapper objectMapper;

    public SepayWebhookService(SepayWebhookTxService tx, ObjectMapper objectMapper) {
        this.tx = tx;
        this.objectMapper = objectMapper;
    }

    /**
     * Nhận một webhook và xử lý tới cùng.
     *
     * <p>Không bao giờ ném ra ngoài: nhà cung cấp chỉ cần biết "đã nhận". Mọi
     * hỏng hóc được ghi vào {@code payment_webhook_events} dưới dạng
     * {@code ERROR} — và {@code ERROR} là trạng thái XỬ LÝ LẠI ĐƯỢC, nên lần
     * gửi lại của SePay sẽ thử lại chính khoản tiền đó.
     */
    public WebhookProcessingResult handle(String rawBody) {
        SepayWebhookPayload payload = parse(rawBody);
        if (payload == null || payload.externalId() == null) {
            return recordUnparseable(rawBody);
        }

        String externalId = payload.externalId();
        SepayWebhookTxService.Claim claim;
        try {
            claim = tx.claim(externalId, rawBody);
        } catch (DataIntegrityViolationException e) {
            // Hai bản sao của cùng một sự kiện về song song; khoá duy nhất
            // (provider, external_id) đã chặn bản thứ hai. Đúng ý đồ.
            log.info("Webhook {} về trùng lúc với một bản sao — bỏ qua bản này", externalId);
            return WebhookProcessingResult.DUPLICATE;
        }
        if (claim.alreadyDone()) {
            return WebhookProcessingResult.DUPLICATE;
        }

        try {
            return tx.process(claim.eventId(), payload);
        } catch (RuntimeException e) {
            log.error("Xử lý webhook {} hỏng — sự kiện được giữ ở trạng thái ERROR để xử lý lại",
                    externalId, e);
            tx.recordError(claim.eventId(), e.getClass().getSimpleName() + ": " + e.getMessage());
            return WebhookProcessingResult.ERROR;
        }
    }

    /** So khoá webhook theo kiểu chống đo thời gian. */
    public static boolean apiKeyMatches(String presented, String expected) {
        if (presented == null || expected == null) {
            return false;
        }
        // MessageDigest.isEqual so hết mảng chứ không dừng ở byte lệch đầu
        // tiên. String.equals thì dừng — và thời gian trả lời khác nhau theo số
        // ký tự đầu đúng là đủ để dò ra khoá từng ký tự một.
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private SepayWebhookPayload parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, SepayWebhookPayload.class);
        } catch (Exception e) {
            log.error("Không đọc được thân webhook SePay: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Thân request không đọc được vẫn phải để lại bằng chứng.
     *
     * <p>Bọc nguyên văn vào một object JSON hợp lệ rồi ghi như mọi sự kiện
     * khác. Bỏ qua nó là quay về đúng cái tình huống cần tránh: nhà cung cấp
     * báo đã gửi, hệ thống không có gì để đối chiếu.
     *
     * <p>Khoá chống trùng lấy từ băm của chính thân request, nên nhà cung cấp
     * gửi lại đúng nội dung đó cũng không sinh thêm dòng rác.
     */
    private WebhookProcessingResult recordUnparseable(String rawBody) {
        String externalId = "malformed:" + sha256Prefix(rawBody);
        try {
            String wrapped = objectMapper.writeValueAsString(
                    Map.of("_unparsed", rawBody == null ? "" : rawBody));
            SepayWebhookTxService.Claim claim = tx.claim(externalId, wrapped);
            if (!claim.alreadyDone()) {
                tx.recordError(claim.eventId(),
                        "Thân webhook không đọc được hoặc thiếu id/referenceCode");
            }
        } catch (Exception e) {
            log.error("Không ghi được sự kiện webhook hỏng: {}", e.getMessage());
        }
        return WebhookProcessingResult.UNMATCHED;
    }

    private static String sha256Prefix(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException("JVM không có SHA-256", e);
        }
    }
}
