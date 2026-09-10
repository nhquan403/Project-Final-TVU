package com.tvh.homestay.booking;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Sinh mã đơn và mã truy cập.
 *
 * <p><b>Bắt buộc dùng {@link SecureRandom}.</b> {@code java.util.Random} có seed
 * 48 bit và khôi phục được chỉ từ vài giá trị liên tiếp — ai thấy hai ba mã đơn
 * là đoán được mọi mã tương lai, và cùng với đó là mọi mã truy cập.
 *
 * <p><b>Hai chuỗi, hai vai trò khác hẳn nhau.</b> Mã đơn ({@code TVH8F3K2Q}) là
 * thứ để HIỂN THỊ: nó in trên email, đọc qua điện thoại, và nằm trong nội dung
 * chuyển khoản nên xuất hiện trên sao kê ngân hàng lẫn dashboard của nhà cung
 * cấp thanh toán. Không thể coi nó là bí mật. Bí mật thao tác là
 * {@code accessToken} — 32 ký tự hex, chỉ đi qua link gửi cho khách.
 */
@Component
public class BookingCodeGenerator {

    /**
     * Bảng chữ Crockford Base32: đã bỏ I, L, O, U.
     *
     * <p>I và 1, O và 0 dễ đọc nhầm khi khách đọc mã qua điện thoại; U bị bỏ để
     * mã ngẫu nhiên không vô tình tạo ra từ thô tục.
     */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final String PREFIX = "TVH";
    private static final int CODE_LENGTH = 6;
    private static final int ACCESS_TOKEN_BYTES = 16; // 16 byte = 32 ký tự hex

    private final SecureRandom random = new SecureRandom();

    /** {@code TVH} + 6 ký tự Base32, ví dụ {@code TVH8F3K2Q}. */
    public String newCode() {
        StringBuilder code = new StringBuilder(PREFIX);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }

    public String newAccessToken() {
        byte[] buffer = new byte[ACCESS_TOKEN_BYTES];
        random.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }

    /**
     * Nội dung chuyển khoản: mã đơn + hai chữ số lần thử.
     *
     * <p>Hậu tố lần thử là thứ khiến QR của lần thanh toán trước không khớp
     * nhầm vào lần sau. Định dạng này được Phase 6 dùng để dựng QR và để đối
     * soát webhook, nên nó là hợp đồng — đổi ở đây là làm hỏng đối soát.
     */
    public static String transferContent(String bookingCode, int attemptNo) {
        return bookingCode + String.format("%02d", attemptNo);
    }
}
