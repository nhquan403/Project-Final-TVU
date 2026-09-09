package com.tvh.homestay.auth;

import com.tvh.homestay.user.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Đọc {@code Authorization: Bearer}, kiểm chữ ký, hạn, và PHIÊN BẢN TOKEN.
 *
 * <p>Bước kiểm phiên bản là điểm khác biệt so với một filter JWT thông thường:
 * chữ ký đúng và chưa hết hạn vẫn chưa đủ. Claim {@code tv} phải khớp
 * {@code users.token_version} hiện tại, nếu không thì token đã bị thu hồi
 * (đăng xuất, đổi mật khẩu, khoá tài khoản, đổi vai trò) và phải từ chối ngay.
 *
 * <p>Token hỏng KHÔNG làm request thất bại tại đây — filter chỉ đơn giản không
 * nạp danh tính, và chuỗi phân quyền phía sau quyết định 401 hay 403. Ném lỗi ở
 * đây sẽ chặn cả những endpoint công khai vốn không cần token.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwt;
    private final TokenVersionCache tokenVersions;

    public JwtAuthenticationFilter(JwtService jwt, TokenVersionCache tokenVersions) {
        this.jwt = jwt;
        this.tokenVersions = tokenVersions;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        Jwt decoded = jwt.decodeOrNull(token);
        if (decoded == null) {
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(decoded.getSubject());
        } catch (NumberFormatException e) {
            return;
        }

        Integer current = tokenVersions.currentTokenVersion(userId);
        Object claimed = decoded.getClaim(JwtService.CLAIM_TOKEN_VERSION);
        if (current == null || claimed == null || current != ((Number) claimed).intValue()) {
            // Token đã bị thu hồi, hoặc người dùng không còn tồn tại.
            return;
        }

        UserRole role;
        try {
            role = UserRole.valueOf(decoded.getClaimAsString(JwtService.CLAIM_ROLE));
        } catch (IllegalArgumentException | NullPointerException e) {
            return;
        }

        var principal = new AuthenticatedUser(userId, decoded.getSubject(), role, false);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
