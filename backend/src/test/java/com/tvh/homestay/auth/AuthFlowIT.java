package com.tvh.homestay.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.schema.AbstractPostgresIT;
import com.tvh.homestay.user.entity.UserRole;
import com.tvh.homestay.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Luồng xác thực đi qua HTTP thật, không gọi thẳng service.
 *
 * <p>Đi qua cổng thật là bắt buộc ở đây: phần lớn thứ cần chứng minh nằm trong
 * chuỗi filter (kiểm phiên bản token, ma trận phân quyền, cookie), và gọi
 * service trực tiếp sẽ nhảy qua đúng phần đó.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIT extends AbstractPostgresIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenService refreshTokens;

    private String email;
    private String clientIp;

    @BeforeEach
    void freshUser() {
        // Email khác nhau mỗi test: các test dùng chung một container, và
        // giới hạn tần suất theo email sẽ dồn lại nếu dùng lại một địa chỉ.
        email = "kh" + System.nanoTime() + "@example.com";
        // IP giả lập khác nhau mỗi test, VÌ giới hạn theo IP là thật và đúng.
        // Không nới hạn mức cho môi trường test — làm vậy là kiểm một cấu hình
        // khác với cấu hình chạy thật. Thay vào đó mỗi test đóng vai một máy
        // khách riêng, đúng như ngoài đời.
        clientIp = "198.51.100." + (COUNTER.incrementAndGet() % 250 + 1);
    }

    private static final java.util.concurrent.atomic.AtomicInteger COUNTER =
            new java.util.concurrent.atomic.AtomicInteger();

    /** Mọi request của test đều đi kèm IP riêng của test đó. */
    private HttpHeaders headers(HttpHeaders base) {
        HttpHeaders h = base == null ? new HttpHeaders() : base;
        if (!h.containsKey("X-Forwarded-For")) {
            h.add("X-Forwarded-For", clientIp);
        }
        return h;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private ResponseEntity<Map> post(String path, Object body, HttpHeaders base) {
        HttpHeaders h = headers(base);
        h.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
    }

    private <T> ResponseEntity<T> get(String path, HttpHeaders base, Class<T> type) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers(base)), type);
    }

    private ResponseEntity<Map> register(String address, Map<String, Object> extra) {
        var body = new java.util.HashMap<String, Object>(Map.of(
                "email", address, "password", "MatKhauRatManh1", "fullName", "Khách Thử"));
        body.putAll(extra);
        return post("/api/auth/register", body, null);
    }

    private static HttpHeaders bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private static String cookieFrom(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).as("phản hồi phải kèm cookie refresh token").isNotNull();
        return cookies.stream()
                .filter(c -> c.startsWith("refresh_token="))
                .findFirst()
                .orElseThrow();
    }

    private static HttpHeaders withCookie(String setCookieHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookieHeader.split(";", 2)[0]);
        return headers;
    }

    // ── Luồng đầy đủ ───────────────────────────────────────────────────────
    @Test
    @DisplayName("Đăng ký → đăng nhập → /api/me → refresh → logout, token cũ chết ngay")
    void fullLifecycle() {
        ResponseEntity<Map> registered = register(email, Map.of());
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> login = post("/api/auth/login",
                Map.of("email", email, "password", "MatKhauRatManh1"), null);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = (String) login.getBody().get("accessToken");
        String refreshCookie = cookieFrom(login);

        ResponseEntity<Map> me = get("/api/me", bearer(accessToken), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("email")).isEqualTo(email);
        assertThat(me.getBody().get("role")).isEqualTo("CUSTOMER");

        ResponseEntity<Map> refreshed = post("/api/auth/refresh", null, withCookie(refreshCookie));
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccessToken = (String) refreshed.getBody().get("accessToken");
        String newRefreshCookie = cookieFrom(refreshed);

        assertThat(get("/api/me", bearer(newAccessToken), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Đăng xuất bằng token MỚI, rồi thử lại chính token đó.
        HttpHeaders logoutHeaders = bearer(newAccessToken);
        logoutHeaders.add(HttpHeaders.COOKIE, newRefreshCookie.split(";", 2)[0]);
        assertThat(post("/api/auth/logout", null, logoutHeaders).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        // Access token còn hạn nhưng token_version đã tăng => chết NGAY, không
        // phải chờ hết 15 phút.
        assertThat(get("/api/me", bearer(newAccessToken), Map.class).getStatusCode())
                .as("access token cũ phải bị từ chối ngay sau đăng xuất")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // Refresh token cũ cũng không dùng lại được.
        assertThat(post("/api/auth/refresh", null, withCookie(newRefreshCookie)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Refresh token đã xoay vòng, dùng lại lần hai bị từ chối và cắt cả họ token")
    void reusedRefreshTokenRevokesWholeFamily() {
        register(email, Map.of());
        ResponseEntity<Map> login = post("/api/auth/login",
                Map.of("email", email, "password", "MatKhauRatManh1"), null);
        String firstCookie = cookieFrom(login);

        ResponseEntity<Map> refreshed = post("/api/auth/refresh", null, withCookie(firstCookie));
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String secondCookie = cookieFrom(refreshed);

        // Dùng LẠI token đầu tiên: bản hợp lệ chỉ có một, nên hai bên cùng dùng
        // nghĩa là có bên thứ hai đang giữ bản sao.
        assertThat(post("/api/auth/refresh", null, withCookie(firstCookie)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // Cả họ token bị cắt: token thứ hai — vốn hợp lệ — cũng chết theo.
        assertThat(post("/api/auth/refresh", null, withCookie(secondCookie)).getStatusCode())
                .as("token kế nhiệm cũng phải bị thu hồi")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Khoá tài khoản làm token đang lưu hành mất hiệu lực ngay")
    void disablingAccountKillsLiveTokens() {
        register(email, Map.of());
        String accessToken = (String) post("/api/auth/login",
                Map.of("email", email, "password", "MatKhauRatManh1"), null)
                .getBody().get("accessToken");

        assertThat(get("/api/me", bearer(accessToken), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Khoá tài khoản đi qua ĐÚNG đường mà màn hình quản trị ở Phase 7 sẽ
        // đi: đổi cờ enabled rồi cắt mọi phiên. Bước thứ hai mới là bước làm
        // token đang lưu hành chết ngay; thiếu nó thì token vẫn sống thêm tối
        // đa 15 phút sau khi tài khoản đã bị khoá.
        jdbc.update("UPDATE users SET enabled = false WHERE email = ?", email);
        refreshTokens.revokeAllSessions(users.findByEmail(email).orElseThrow().getId());

        assertThat(get("/api/me", bearer(accessToken), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Leo thang đặc quyền ────────────────────────────────────────────────
    @Test
    @DisplayName("Client gửi role=ADMIN lúc đăng ký vẫn chỉ tạo được CUSTOMER")
    void clientCannotChooseItsOwnRole() {
        assertThat(register(email, Map.of("role", "ADMIN")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(users.findByEmail(email).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("/api/admin/**: 401 khi không token, 403 với CUSTOMER, 200 với ADMIN")
    void adminEndpointsCheckRoleNotJustPresenceOfToken() {
        register(email, Map.of());
        String customerToken = (String) post("/api/auth/login",
                Map.of("email", email, "password", "MatKhauRatManh1"), null)
                .getBody().get("accessToken");

        assertThat(get("/api/admin/ping", null, String.class).getStatusCode())
                .as("không token")
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(get("/api/admin/ping", bearer(customerToken), String.class).getStatusCode())
                .as("token CUSTOMER")
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Nâng quyền rồi đăng nhập lại. Cắt phiên là phần bắt buộc: token cũ
        // mang vai trò CUSTOMER, để nó sống tiếp nghĩa là hạ quyền một ADMIN
        // cũng sẽ không có hiệu lực ngay.
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        refreshTokens.revokeAllSessions(users.findByEmail(email).orElseThrow().getId());
        String adminToken = (String) post("/api/auth/login",
                Map.of("email", email, "password", "MatKhauRatManh1"), null)
                .getBody().get("accessToken");

        // /api/admin/ping chưa tồn tại ở phase này: qua được phân quyền thì
        // dừng ở 404, KHÔNG phải 401/403. Đó chính là điều cần chứng minh.
        assertThat(get("/api/admin/ping", bearer(adminToken), String.class).getStatusCode())
                .as("token ADMIN qua được lớp phân quyền")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Cookie ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Cookie refresh token có đủ HttpOnly, Secure, SameSite=Strict")
    void refreshCookieIsHardened() {
        register(email, Map.of());
        String cookie = cookieFrom(post("/api/auth/login",
                Map.of("email", email, "password", "MatKhauRatManh1"), null));

        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("Secure");
        assertThat(cookie).contains("SameSite=Strict");
        // Chỉ gửi kèm cho nhánh /api/auth, không rò sang mọi request khác.
        assertThat(cookie).contains("Path=/api/auth");
    }

    // ── Giới hạn tần suất ──────────────────────────────────────────────────
    @Test
    @DisplayName("Đổi X-Forwarded-For mỗi lần vẫn bị chặn: khoá theo email không phụ thuộc IP")
    void spoofingClientIpDoesNotEvadeThePerEmailLimit() {
        register(email, Map.of());

        HttpStatus lastStatus = null;
        for (int attempt = 1; attempt <= 11; attempt++) {
            HttpHeaders headers = new HttpHeaders();
            // Mỗi lần một IP khác. Khoá theo IP vì thế vô hiệu — đúng kịch bản
            // kẻ tấn công tự đặt header khi reverse proxy không ghi đè nó.
            headers.add("X-Forwarded-For", "203.0.113." + attempt);
            lastStatus = (HttpStatus) post("/api/auth/login",
                    Map.of("email", email, "password", "SaiMatKhau999"), headers).getStatusCode();
        }
        assertThat(lastStatus)
                .as("lần thứ 11 phải bị khoá theo email")
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
