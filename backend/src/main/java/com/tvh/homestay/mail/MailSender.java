package com.tvh.homestay.mail;

/**
 * Đường ra của một email đã dựng xong.
 *
 * <p>Có đúng hai cài đặt: {@link SmtpMailSender} khi có máy chủ SMTP, và
 * {@link LoggingMailSender} khi không. Cả hai đều được gọi từ cùng một chỗ và
 * đều để lại dấu vết trong {@code outbound_emails}, nên câu hỏi "thư này đã
 * gửi chưa" luôn trả lời được bằng SQL, kể cả ở bản demo không cấu hình SMTP.
 */
public interface MailSender {

    /**
     * Gửi đi, hoặc ném ngoại lệ.
     *
     * <p>Ném là cách DUY NHẤT báo hỏng: nuốt lỗi rồi trả về bình thường sẽ
     * khiến hộp thư đi ghi {@code SENT} cho một lá thư không ai nhận được.
     */
    void send(String to, String subject, String html);

    /** Tên hiển thị trong log và trong báo cáo, để biết bản đang chạy dùng đường nào. */
    String describe();
}
