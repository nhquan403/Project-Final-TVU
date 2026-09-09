package com.tvh.homestay.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Sinh danh sách khoá giới hạn tần suất cho một request, theo ma trận của
 * Phase 4.
 *
 * <p><b>Vì sao mỗi endpoint nhạy cảm có HAI khoá.</b> Khoá theo IP là lớp
 * thông thường, nhưng nó chỉ đúng khi IP đọc được là IP thật. Trong bản đóng
 * gói, request đi qua nginx rồi mới tới API; nếu nginx không ghi đè
 * {@code X-Forwarded-For}, kẻ tấn công tự đặt header đó và dò không giới hạn.
 * Nên mọi endpoint đáng bị dò đều có thêm một khoá KHÔNG phụ thuộc IP: email
 * cho đăng nhập, mã đơn cho thao tác booking, số điện thoại cho tạo đơn. Lớp
 * này còn tác dụng kể cả khi IP bị giả mạo hoàn toàn.
 */
@Component
public class RateLimitKeyResolver {

    /** Một hạn mức: khoá, sức chứa, và chu kỳ nạp lại. */
    public record Limit(String key, int capacity, Duration window) {}

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private static final Pattern BOOKING_CODE =
            Pattern.compile("^/api/bookings/([^/]+)/(payment-status|cancel|review)$");

    private final ObjectMapper objectMapper;

    public RateLimitKeyResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Endpoint nào cần đọc thân request để lấy khoá. */
    public boolean needsBody(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "POST".equals(request.getMethod())
                && (path.equals("/api/auth/login")
                        || path.equals("/api/bookings")
                        || path.equals("/api/bookings/lookup"));
    }

    public List<Limit> resolve(HttpServletRequest request, byte[] body) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = request.getRemoteAddr();
        List<Limit> limits = new ArrayList<>();

        Matcher bookingCode = BOOKING_CODE.matcher(path);

        if (path.startsWith("/api/auth/")) {
            limits.add(new Limit("ip:auth:" + ip, 10, ONE_MINUTE));
            if (path.equals("/api/auth/login")) {
                // Khoá thứ hai, không phụ thuộc IP: đổi IP mỗi lần vẫn không
                // dò được mật khẩu của một email cụ thể.
                readField(body, "email").ifPresent(
                        email -> limits.add(new Limit("email:login:" + email.toLowerCase(), 5, ONE_MINUTE)));
            }
        } else if (path.equals("/api/availability")) {
            limits.add(new Limit("ip:availability:" + ip, 60, ONE_MINUTE));
        } else if (path.equals("/api/promotions/check")) {
            limits.add(new Limit("ip:promo:" + ip, 20, ONE_MINUTE));
        } else if (path.equals("/api/bookings") && "POST".equals(method)) {
            limits.add(new Limit("ip:booking-create:" + ip, 10, ONE_MINUTE));
            readField(body, "guestPhone")
                    .ifPresent(phone -> limits.add(new Limit("phone:booking:" + phone, 10, ONE_HOUR)));
        } else if (path.equals("/api/bookings/lookup")) {
            limits.add(new Limit("ip:lookup:" + ip, 10, ONE_MINUTE));
            readField(body, "code")
                    .ifPresent(code -> limits.add(new Limit("code:lookup:" + code, 5, ONE_HOUR)));
        } else if (bookingCode.matches()) {
            String code = bookingCode.group(1);
            String action = bookingCode.group(2);
            if ("payment-status".equals(action)) {
                limits.add(new Limit("code:payment-status:" + code, 60, ONE_MINUTE));
            } else if ("cancel".equals(action)) {
                limits.add(new Limit("ip:cancel:" + ip, 10, ONE_MINUTE));
                limits.add(new Limit("code:cancel:" + code, 5, ONE_HOUR));
            } else {
                limits.add(new Limit("ip:review:" + ip, 5, ONE_MINUTE));
            }
        }
        return limits;
    }

    private java.util.Optional<String> readField(byte[] body, String field) {
        if (body == null || body.length == 0) {
            return java.util.Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(body).get(field);
            return node == null || node.isNull()
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(node.asText());
        } catch (Exception e) {
            // Thân request hỏng thì để tầng validate báo lỗi; ở đây chỉ mất
            // khoá phụ, khoá theo IP vẫn còn tác dụng.
            return java.util.Optional.empty();
        }
    }
}
