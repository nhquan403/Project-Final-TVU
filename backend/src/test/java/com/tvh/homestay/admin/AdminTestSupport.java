package com.tvh.homestay.admin;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dựng một tài khoản ADMIN thật cho test.
 *
 * <p>Đi đúng đường mà người thật đi: đăng ký qua API rồi nâng quyền bằng SQL.
 * Dữ liệu mẫu là việc của Phase 9, nên chưa có tài khoản quản trị sẵn — và tự
 * chèn thẳng một dòng {@code users} bằng SQL sẽ bỏ qua việc băm mật khẩu, tức
 * là test chứng minh một luồng đăng nhập không tồn tại ngoài đời.
 *
 * <p>Mật khẩu sinh NGẪU NHIÊN mỗi lần chạy và không bao giờ được ghi vào repo.
 */
public final class AdminTestSupport {

    private AdminTestSupport() {}

    /** Token của một tài khoản và vai trò của nó. */
    public record Session(String email, String accessToken) {}

    public static Session registerAdmin(
            TestRestTemplate rest, int port, JdbcTemplate jdbc, String clientIp) {
        Session session = registerCustomer(rest, port, jdbc, clientIp);
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", session.email());
        // Đăng nhập LẠI sau khi nâng quyền: vai trò nằm trong token, nên token
        // cấp trước lúc nâng quyền vẫn là token của CUSTOMER.
        return new Session(session.email(), login(rest, port, session.email(), passwordOf(session), clientIp));
    }

    public static Session registerCustomer(
            TestRestTemplate rest, int port, JdbcTemplate jdbc, String clientIp) {
        String email = "tk" + System.nanoTime() + "@tvh.local";
        String password = randomPassword();
        PASSWORDS.put(email, password);

        rest.exchange(
                "http://localhost:" + port + "/api/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", email,
                        "password", password,
                        "fullName", "Tài khoản kiểm thử",
                        "phone", "0900000000"), jsonHeaders(clientIp)),
                Map.class);

        return new Session(email, login(rest, port, email, password, clientIp));
    }

    public static HttpHeaders bearer(String token, String clientIp) {
        HttpHeaders headers = jsonHeaders(clientIp);
        headers.setBearerAuth(token);
        return headers;
    }

    public static HttpHeaders jsonHeaders(String clientIp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // IP riêng cho từng lớp test: hạn mức tần suất của Phase 4 là thật và
        // đúng, không nới cho test.
        headers.add("X-Forwarded-For", clientIp);
        return headers;
    }

    private static final Map<String, String> PASSWORDS = new java.util.concurrent.ConcurrentHashMap<>();

    private static String passwordOf(Session session) {
        return PASSWORDS.get(session.email());
    }

    @SuppressWarnings("unchecked")
    private static String login(
            TestRestTemplate rest, int port, String email, String password, String clientIp) {
        Map<String, Object> body = rest.exchange(
                "http://localhost:" + port + "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), jsonHeaders(clientIp)),
                Map.class).getBody();
        if (body == null || body.get("accessToken") == null) {
            throw new IllegalStateException("Không đăng nhập được tài khoản kiểm thử " + email);
        }
        return String.valueOf(body.get("accessToken"));
    }

    /** Đủ mạnh để qua mọi ràng buộc mật khẩu, và khác nhau mỗi lần chạy. */
    private static String randomPassword() {
        byte[] buffer = new byte[18];
        new SecureRandom().nextBytes(buffer);
        return "Aa1!" + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
