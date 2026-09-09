package com.tvh.homestay.auth;

import com.tvh.homestay.auth.dto.AuthDtos.ChangePasswordRequest;
import com.tvh.homestay.auth.dto.AuthDtos.LoginRequest;
import com.tvh.homestay.auth.dto.AuthDtos.MeResponse;
import com.tvh.homestay.auth.dto.AuthDtos.RegisterRequest;
import com.tvh.homestay.auth.dto.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sáu endpoint xác thực.
 *
 * <p>Access token đi trong thân phản hồi (frontend giữ trong bộ nhớ), refresh
 * token đi trong cookie {@code HttpOnly}. Chia đôi như vậy là có lý do: refresh
 * token sống 7 ngày, để nó trong {@code localStorage} nghĩa là bất kỳ lỗ hổng
 * XSS nào — kể cả một file SVG tải lên rồi phục vụ cùng origin — cũng đọc được
 * và chiếm phiên trong cả tuần. Cơ chế phát hiện tái sử dụng không cứu được
 * tình huống đó, vì kẻ tấn công mới là bên dùng token còn nạn nhân là bên bị
 * thu hồi.
 */
@RestController
public class AuthController {

    /** Cookie chỉ gửi kèm cho nhánh /api/auth — không rò sang mọi request khác. */
    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService auth;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    public AuthController(AuthService auth, JwtService jwt, RefreshTokenService refreshTokens) {
        this.auth = auth;
        this.jwt = jwt;
        this.refreshTokens = refreshTokens;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = auth.register(request);
        return respondWithTokens(auth.issuePair(user));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return respondWithTokens(auth.login(request));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        return respondWithTokens(auth.refresh(refreshToken));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        auth.logout(principal.id(), refreshToken);
        // Xoá cookie phía trình duyệt bằng cookie cùng tên hết hạn ngay.
        ResponseCookie cleared = buildRefreshCookie("", Duration.ZERO);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }

    @PostMapping("/api/auth/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(principal.id(), request);
        ResponseCookie cleared = buildRefreshCookie("", Duration.ZERO);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return auth.me(principal.id());
    }

    private ResponseEntity<TokenResponse> respondWithTokens(AuthService.TokenPair pair) {
        ResponseCookie cookie = buildRefreshCookie(pair.refreshToken(), refreshTokens.getTtl());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponse(pair.accessToken(), jwt.getAccessTokenTtl().toSeconds()));
    }

    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                // HttpOnly: JavaScript không đọc được, kể cả khi trang bị XSS.
                .httpOnly(true)
                // Secure kể cả lúc dev: trình duyệt vẫn chấp nhận cookie Secure
                // trên http://localhost vì localhost được coi là origin tin cậy.
                .secure(true)
                // Strict: cookie không đi kèm request khởi phát từ site khác,
                // nên endpoint refresh không bị CSRF lợi dụng.
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
