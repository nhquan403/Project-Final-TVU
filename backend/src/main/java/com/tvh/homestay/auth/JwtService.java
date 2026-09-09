package com.tvh.homestay.auth;

import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;
import com.tvh.homestay.user.entity.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Phát và đọc access token.
 *
 * <p>Ba claim quan trọng: {@code sub} (id người dùng), {@code role}, và
 * {@code tv} — phiên bản token.
 *
 * <p><b>Vì sao có {@code tv}.</b> Access token là stateless: một khi đã phát,
 * nó hợp lệ cho tới lúc hết hạn và máy chủ không có cách nào rút lại. Nghĩa là
 * đăng xuất, khoá tài khoản hay hạ quyền ADMIN sẽ chỉ có hiệu lực sau tối đa
 * 15 phút. Với hệ thống giữ tiền cọc và dữ liệu khách hàng, 15 phút là quá dài.
 * Claim {@code tv} mang giá trị {@code users.token_version} lúc phát; filter so
 * lại với giá trị trong cơ sở dữ liệu mỗi request, nên chỉ cần tăng một số
 * nguyên là mọi token đang lưu hành của người đó chết ngay.
 *
 * <p>Khoá ký đọc từ {@code JWT_SECRET}. Không có giá trị mặc định ở bất kỳ
 * đâu — {@code RequiredSecretsValidator} đã dừng khởi động khi thiếu.
 */
@Service
public class JwtService {

    /** Tên claim mang phiên bản token. Ngắn vì nó nằm trong MỌI request. */
    public static final String CLAIM_TOKEN_VERSION = "tv";
    public static final String CLAIM_ROLE = "role";

    private static final String ISSUER = "homestay-tvh";
    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${JWT_SECRET}") String secret,
            @Value("${auth.access-token-ttl:PT15M}") Duration accessTokenTtl,
            Clock clock) {
        var key = new OctetSequenceKey.Builder(secret.getBytes(StandardCharsets.UTF_8))
                .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
                .build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        this.decoder = NimbusJwtDecoder.withSecretKey(key.toSecretKey())
                .macAlgorithm(ALGORITHM)
                .build();
        this.clock = clock;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String issueAccessToken(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(ALGORITHM).build(), claims))
                .getTokenValue();
    }

    /** Trả {@code null} khi token hỏng, sai chữ ký hoặc hết hạn — nơi gọi quyết định phản ứng. */
    public Jwt decodeOrNull(String token) {
        try {
            return decoder.decode(token);
        } catch (JwtException e) {
            return null;
        }
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }
}
