package com.tvh.homestay.mail;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Dựng nội dung thư từ mẫu Thymeleaf rồi giao cho {@link MailSender}.
 *
 * <p>Tiêu đề nằm ở đây chứ không nằm trong dữ liệu đã xếp hàng: sửa câu chữ
 * tiêu đề sau này không được phép đòi xếp hàng lại những lá thư đang chờ gửi.
 */
@Service
public class MailService {

    public static final String TEMPLATE_CONFIRMED = "booking-confirmed";
    public static final String TEMPLATE_PARTIAL = "booking-partial";
    public static final String TEMPLATE_CANCELLED = "booking-cancelled";

    private static final Map<String, String> SUBJECTS = Map.of(
            TEMPLATE_CONFIRMED, "Xác nhận đặt phòng %s — Homestay TVH",
            TEMPLATE_PARTIAL, "Cần bổ sung thanh toán cho đơn %s — Homestay TVH",
            TEMPLATE_CANCELLED, "Đơn đặt phòng %s đã được huỷ — Homestay TVH");

    private final TemplateEngine templateEngine;
    private final MailSender sender;

    public MailService(TemplateEngine templateEngine, MailSender sender) {
        this.templateEngine = templateEngine;
        this.sender = sender;
    }

    /** Dựng và gửi. Ném ngoại lệ khi hỏng — nơi gọi đếm số lần thử. */
    public void send(String template, String toEmail, Map<String, Object> payload) {
        String subject = subjectFor(template, payload);
        Context context = new Context(new java.util.Locale.Builder().setLanguage("vi").build());
        payload.forEach(context::setVariable);
        String html = templateEngine.process("mail/" + template, context);
        sender.send(toEmail, subject, html);
    }

    public String describeSender() {
        return sender.describe();
    }

    private static String subjectFor(String template, Map<String, Object> payload) {
        String pattern = SUBJECTS.get(template);
        if (pattern == null) {
            throw new IllegalArgumentException("Mẫu thư không tồn tại: " + template);
        }
        Object code = payload.get("code");
        return pattern.formatted(code == null ? "" : code.toString());
    }
}
