package com.tvh.homestay.config;

import com.tvh.homestay.mail.LoggingMailSender;
import com.tvh.homestay.mail.MailSender;
import com.tvh.homestay.mail.SmtpMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Chọn đường gửi thư theo cấu hình, KHÔNG theo profile.
 *
 * <p>Chọn theo profile là sai ở đúng chỗ quan trọng: bản {@code demo} đem đi
 * bảo vệ có lúc có SMTP (Mailpit trong docker compose) và có lúc không (chạy
 * tay trên máy giám khảo). Điều kiện thật là "có {@code MAIL_HOST} hay không",
 * nên đó mới là thứ được kiểm ở đây.
 *
 * <p>{@link ObjectProvider} chứ không tiêm thẳng {@link JavaMailSender}: khi
 * {@code MAIL_HOST} rỗng, bean đó vẫn tồn tại do auto-config của Spring Boot
 * nhưng trỏ tới một host rỗng — lấy lười rồi chỉ dùng khi thật sự có host là
 * cách duy nhất không phải phụ thuộc vào thứ tự nạp auto-config.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    /**
     * Tên bean là {@code homestayMailSender} chứ không phải {@code mailSender}:
     * auto-config của Spring Boot đã đăng ký một bean tên {@code mailSender}
     * (chính là {@link JavaMailSender}), và trùng tên khiến context không dựng
     * được. Đổi tên ở đây rẻ hơn nhiều so với bật
     * {@code spring.main.allow-bean-definition-overriding}, thứ sẽ âm thầm cho
     * qua mọi lần trùng tên khác về sau.
     */
    @Bean
    public MailSender homestayMailSender(
            ObjectProvider<JavaMailSender> javaMailSender,
            @Value("${MAIL_HOST:}") String host,
            @Value("${mail.from:no-reply@homestaytvh.vn}") String from) {

        JavaMailSender delegate = javaMailSender.getIfAvailable();
        if (host.isBlank() || delegate == null) {
            log.warn("Chưa cấu hình MAIL_HOST — thư sẽ được ghi ra log thay vì gửi qua SMTP. "
                    + "Trạng thái gửi vẫn ghi vào bảng outbound_emails.");
            return new LoggingMailSender();
        }
        log.info("Gửi thư qua SMTP tại {}", host);
        return new SmtpMailSender(delegate, from, host);
    }
}
