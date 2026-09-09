package com.tvh.homestay.auth;

import com.tvh.homestay.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Chặn mọi thao tác khác khi tài khoản đang bị buộc đổi mật khẩu.
 *
 * <p>Cờ {@code must_change_password} bật cho tài khoản admin dựng sẵn: mật khẩu
 * ban đầu đi qua tay người khác (file cấu hình, tin nhắn), nên nó phải hết giá
 * trị ngay sau lần đăng nhập đầu. Cho phép dùng tiếp khi chưa đổi là biến mật
 * khẩu tạm thành mật khẩu vĩnh viễn.
 *
 * <p>Hai đường được để mở, vì thiếu chúng thì người dùng không có cách nào
 * thoát ra: xem thông tin của chính mình, và chính thao tác đổi mật khẩu.
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS =
            Set.of("/api/me", "/api/auth/change-password", "/api/auth/logout");

    private final UserRepository users;

    public MustChangePasswordFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser principal
                && !ALLOWED_PATHS.contains(request.getRequestURI())
                && users.findById(principal.id()).map(u -> u.isMustChangePassword()).orElse(false)) {

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            // Không có dòng này, getWriter() dùng ISO-8859-1 và mọi thông
            // báo tiếng Việt về tới client thành ký tự hỏng.
            response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
            response.getWriter().write("""
                    {"type":"about:blank","title":"PASSWORD_CHANGE_REQUIRED","status":403,\
                    "detail":"Phải đổi mật khẩu trước khi dùng tiếp."}""");
            return;
        }
        chain.doFilter(request, response);
    }
}
