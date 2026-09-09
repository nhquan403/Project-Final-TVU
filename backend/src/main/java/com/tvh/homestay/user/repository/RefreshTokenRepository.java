package com.tvh.homestay.user.repository;

import com.tvh.homestay.user.entity.RefreshToken;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Refresh token đã băm, có xoay vòng và phát hiện tái sử dụng. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Thu hồi toàn bộ token còn hiệu lực của một người dùng.
     *
     * <p>Dùng khi phát hiện một token đã bị thay thế lại được đem đi dùng: lúc
     * đó không thể biết bên nào là chủ thật và bên nào là kẻ đánh cắp, nên
     * phản ứng đúng là cắt cả họ token và bắt đăng nhập lại.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken t set t.revokedAt = :now
             where t.user.id = :userId and t.revokedAt is null
            """)
    int revokeAllActiveByUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
