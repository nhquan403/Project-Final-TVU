package com.tvh.homestay.user.entity;

import com.tvh.homestay.common.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Chỉ lưu BĂM SHA-256 của refresh token, không bao giờ lưu token gốc.
 *
 * <p>{@code replacedBy} ghi băm của token kế nhiệm sau mỗi lần xoay vòng. Nếu
 * một token đã bị thay thế lại được đem đi dùng, đó là dấu hiệu token bị đánh
 * cắp — phản ứng đúng là thu hồi cả chuỗi, không chỉ từ chối lần đó.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** NULL nghĩa là token còn hiệu lực. */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "replaced_by", length = 64)
    private String replacedBy;
}
