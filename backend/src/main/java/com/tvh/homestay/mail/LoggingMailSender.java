package com.tvh.homestay.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đường ra khi chưa cấu hình SMTP: ghi TOÀN BỘ nội dung thư ra log.
 *
 * <p>Không phải một cái mock để cho qua chuyện. Bản đem đi bảo vệ thường chạy
 * không có máy chủ thư; nếu lúc đó hệ thống lặng lẽ bỏ qua email thì tiêu chí
 * "khách nhận được thư xác nhận" không kiểm chứng được. Ghi đủ nội dung ra log
 * cho phép mở log ra đọc đúng lá thư mà khách lẽ ra nhận được, còn dòng
 * {@code outbound_emails} vẫn chuyển sang {@code SENT} như đường SMTP thật.
 */
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(String to, String subject, String html) {
        log.info("""
                [MAIL] Chưa cấu hình MAIL_HOST nên thư không được gửi qua SMTP.
                       Người nhận: {}
                       Tiêu đề  : {}
                       Nội dung :
                {}""", to, subject, html);
    }

    @Override
    public String describe() {
        return "LoggingMailSender (chưa cấu hình MAIL_HOST)";
    }
}
