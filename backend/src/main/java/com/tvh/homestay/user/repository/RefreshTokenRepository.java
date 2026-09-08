package com.tvh.homestay.user.repository;

import com.tvh.homestay.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

/** Refresh token đã băm, có xoay vòng và phát hiện tái sử dụng. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
