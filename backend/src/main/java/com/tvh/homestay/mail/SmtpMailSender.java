package com.tvh.homestay.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/** Đường ra thật: gửi thư HTML qua SMTP. */
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final String from;
    private final String host;

    public SmtpMailSender(JavaMailSender javaMailSender, String from, String host) {
        this.javaMailSender = javaMailSender;
        this.from = from;
        this.host = host;
    }

    @Override
    public void send(String to, String subject, String html) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, java.nio.charset.StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            // Bọc lại nhưng KHÔNG nuốt: bộ gửi ở ngoài đếm số lần thử và ghi
            // last_error, nên lỗi phải nổi lên tới đó.
            throw new IllegalStateException("Không gửi được thư qua SMTP: " + e.getMessage(), e);
        }
    }

    @Override
    public String describe() {
        return "SmtpMailSender (" + host + ")";
    }
}
