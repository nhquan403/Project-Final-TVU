package com.tvh.homestay.auth;

import com.tvh.homestay.auth.dto.AuthDtos.ChangePasswordRequest;
import com.tvh.homestay.auth.dto.AuthDtos.LoginRequest;
import com.tvh.homestay.auth.dto.AuthDtos.MeResponse;
import com.tvh.homestay.auth.dto.AuthDtos.RegisterRequest;
import com.tvh.homestay.user.entity.RefreshToken;
import com.tvh.homestay.user.entity.User;
import com.tvh.homestay.user.entity.UserRole;
import com.tvh.homestay.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Nghiệp vụ xác thực. Controller chỉ lo HTTP và cookie. */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokens;
    private final JwtService jwt;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            RefreshTokenService refreshTokens,
            JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.refreshTokens = refreshTokens;
        this.jwt = jwt;
    }

    /** Cặp token trả về sau đăng nhập hoặc làm mới. */
    public record TokenPair(String accessToken, String refreshToken) {}

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được dùng");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        // Vai trò gán CỨNG ở đây. Không có đường nào để client tự chọn vai trò:
        // RegisterRequest không có trường role, và giá trị này không đọc từ đâu.
        user.setRole(UserRole.CUSTOMER);
        user.setEnabled(true);
        user.setMustChangePassword(false);
        return users.save(user);
    }

    @Transactional
    public TokenPair login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException e) {
            // Một thông báo duy nhất cho cả "không có tài khoản" lẫn "sai mật
            // khẩu": phân biệt hai trường hợp là cho kẻ dò biết email nào có thật.
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }
        User user = users.findByEmail(email).orElseThrow();
        return issuePair(user);
    }

    /**
     * Xoay vòng refresh token.
     *
     * <p>Gặp token đã bị thu hồi nghĩa là có hai bên cùng giữ một bản: bản hợp
     * lệ chỉ có một. Không thể biết bên nào là chủ thật, nên cắt cả họ token và
     * tăng {@code token_version} để access token đang lưu hành cũng chết theo.
     */
    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.find(rawRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token không hợp lệ"));

        if (!refreshTokens.isUsable(stored)) {
            // Thu hồi chạy trong transaction RIÊNG, nên nó vẫn commit dù ngay
            // sau đây ngoại lệ được ném ra để từ chối request.
            refreshTokens.revokeAllSessions(stored.getUser().getId());
            throw new BadCredentialsException("Refresh token đã bị thu hồi");
        }

        User user = stored.getUser();
        RefreshTokenService.IssuedToken issued = refreshTokens.issue(user);
        refreshTokens.markReplaced(stored, issued.stored());
        return new TokenPair(jwt.issueAccessToken(user), issued.rawValue());
    }

    /**
     * Đăng xuất: thu hồi refresh token VÀ tăng {@code token_version}.
     *
     * <p>Chỉ thu hồi refresh token là chưa đủ — access token còn hạn vẫn dùng
     * được thêm tối đa 15 phút nữa. Tăng {@code token_version} là thứ khiến
     * tiêu chí "sau đăng xuất token cũ bị từ chối" thành sự thật.
     */
    @Transactional
    public void logout(Long userId, String rawRefreshToken) {
        if (rawRefreshToken != null) {
            refreshTokens.find(rawRefreshToken).ifPresent(refreshTokens::revoke);
        }
        refreshTokens.revokeAllSessions(userId);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = users.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        users.save(user);

        // Đổi mật khẩu thường là phản ứng khi nghi bị lộ, nên mọi phiên khác
        // phải chết theo — kể cả phiên của kẻ đã chiếm được tài khoản.
        refreshTokens.revokeAllSessions(userId);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = users.findById(userId).orElseThrow();
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.isMustChangePassword());
    }

    public TokenPair issuePair(User user) {
        RefreshTokenService.IssuedToken issued = refreshTokens.issue(user);
        return new TokenPair(jwt.issueAccessToken(user), issued.rawValue());
    }
}
