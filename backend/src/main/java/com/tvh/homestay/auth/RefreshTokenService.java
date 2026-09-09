package com.tvh.homestay.auth;

import com.tvh.homestay.user.entity.RefreshToken;
import com.tvh.homestay.user.entity.User;
import com.tvh.homestay.user.repository.RefreshTokenRepository;
import com.tvh.homestay.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phát, đối chiếu và xoay vòng refresh token.
 *
 * <p><b>Chỉ lưu băm.</b> Cột {@code token_hash} giữ SHA-256 của token, không
 * bao giờ giữ token gốc. Rò cơ sở dữ liệu thì kẻ đọc được cũng không đăng nhập
 * lại được bằng những gì đọc thấy. Không cần thuật toán chậm như BCrypt ở đây:
 * token là 256 bit ngẫu nhiên, không dò từ điển được như mật khẩu người đặt.
 *
 * <p><b>Xoay vòng mỗi lần dùng.</b> Token cũ bị đánh dấu {@code revoked_at} và
 * trỏ {@code replaced_by} sang token kế nhiệm. Nếu một token ĐÃ bị thay thế lại
 * được đem đi dùng, đó là dấu hiệu token bị đánh cắp: bản sao hợp lệ chỉ có một,
 * nên hai bên cùng dùng nghĩa là có bên thứ hai. Phản ứng là cắt cả họ token và
 * tăng {@code token_version} — không thể biết bên nào là chủ thật.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32; // 256 bit

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final TokenVersionCache tokenVersions;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final Duration ttl;

    public RefreshTokenService(
            RefreshTokenRepository tokens,
            UserRepository users,
            TokenVersionCache tokenVersions,
            @Value("${auth.refresh-token-ttl:P7D}") Duration ttl,
            Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.tokenVersions = tokenVersions;
        this.ttl = ttl;
        this.clock = clock;
    }

    /** Giá trị thô chỉ tồn tại trong lượt trả về này; phía lưu trữ chỉ thấy băm. */
    public record IssuedToken(String rawValue, RefreshToken stored) {}

    @Transactional
    public IssuedToken issue(User user) {
        byte[] buffer = new byte[TOKEN_BYTES];
        random.nextBytes(buffer);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(sha256Hex(raw));
        entity.setExpiresAt(OffsetDateTime.now(clock).plus(ttl));
        return new IssuedToken(raw, tokens.save(entity));
    }

    public Optional<RefreshToken> find(String rawValue) {
        return tokens.findByTokenHash(sha256Hex(rawValue));
    }

    /** Đánh dấu token cũ đã bị thay thế bởi token mới. */
    @Transactional
    public void markReplaced(RefreshToken previous, RefreshToken replacement) {
        previous.setRevokedAt(OffsetDateTime.now(clock));
        previous.setReplacedBy(replacement.getTokenHash());
        tokens.save(previous);
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(OffsetDateTime.now(clock));
        tokens.save(token);
    }

    /**
     * Cắt mọi phiên của một người dùng: thu hồi toàn bộ refresh token còn hiệu
     * lực VÀ tăng {@code token_version} để access token đang lưu hành chết theo.
     *
     * <p><b>{@code REQUIRES_NEW} là bắt buộc, không phải trang trí.</b> Nơi gọi
     * quan trọng nhất là lúc phát hiện refresh token bị dùng lại — và ngay sau
     * khi thu hồi, nó NÉM ngoại lệ để từ chối request. Nếu chạy chung
     * transaction với nơi gọi, ngoại lệ đó sẽ cuốn luôn phần thu hồi vào
     * rollback: hệ thống báo lỗi cho kẻ tấn công nhưng token vẫn còn sống
     * nguyên. Transaction riêng khiến việc thu hồi commit trước khi ngoại lệ
     * kịp lan ra.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllSessions(Long userId) {
        tokens.revokeAllActiveByUser(userId, OffsetDateTime.now(clock));
        users.incrementTokenVersion(userId);
        tokenVersions.invalidate(userId);
    }

    public boolean isUsable(RefreshToken token) {
        return token.getRevokedAt() == null && token.getExpiresAt().isAfter(OffsetDateTime.now(clock));
    }

    public Duration getTtl() {
        return ttl;
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e);
        }
    }
}
