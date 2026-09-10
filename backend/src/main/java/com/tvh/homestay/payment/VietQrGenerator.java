package com.tvh.homestay.payment;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dựng ảnh QR VietQR cho một lần thanh toán.
 *
 * <p>Dùng dịch vụ ảnh của SePay thay vì tự mã hoá EMVCo: chuỗi VietQR có CRC16
 * và cây TLV lồng nhau, tự sinh sai một byte thì ứng dụng ngân hàng từ chối mà
 * không nói vì sao. Đổi lại, ảnh QR cần mạng để tải — nên màn hình thanh toán
 * BẮT BUỘC hiển thị thêm số tài khoản và nội dung chuyển khoản dạng chữ để
 * khách gõ tay khi ảnh không lên.
 *
 * <p>Số tài khoản và mã ngân hàng đọc từ biến môi trường, không có giá trị mặc
 * định trong mã nguồn: một số tài khoản "để demo" nằm trong repo công khai là
 * số tài khoản của một người thật.
 */
@Component
public class VietQrGenerator {

    private static final String QR_IMAGE_BASE = "https://qr.sepay.vn/img";

    private final String accountNumber;
    private final String bankCode;

    public VietQrGenerator(
            @Value("${SEPAY_ACCOUNT_NUMBER:}") String accountNumber,
            @Value("${SEPAY_BANK_CODE:}") String bankCode) {
        this.accountNumber = accountNumber;
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBankCode() {
        return bankCode;
    }

    /** Chưa cấu hình tài khoản nhận tiền thì không dựng được QR, và phải nói rõ. */
    public boolean isConfigured() {
        return !accountNumber.isBlank() && !bankCode.isBlank();
    }

    /**
     * URL ảnh QR đã gắn sẵn số tiền và nội dung chuyển khoản.
     *
     * <p>Trả {@code null} khi chưa cấu hình tài khoản — màn hình thanh toán sẽ
     * hiện phần chuyển khoản thủ công thay vì một ảnh hỏng.
     */
    public String imageUrl(BigDecimal amount, String transferContent) {
        if (!isConfigured()) {
            return null;
        }
        return QR_IMAGE_BASE
                + "?acc=" + encode(accountNumber)
                + "&bank=" + encode(bankCode)
                + "&amount=" + amount.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
                + "&des=" + encode(transferContent);
    }

    /** Nội dung dạng chữ để khách tự nhập khi không quét được QR. */
    public String plainInstruction(BigDecimal amount, String transferContent) {
        return "Chuyển khoản %s đến %s (%s) với nội dung %s"
                .formatted(amount.toPlainString(), accountNumber, bankCode, transferContent);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
