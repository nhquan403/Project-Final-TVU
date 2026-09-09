package com.tvh.homestay.auth;

import com.tvh.homestay.user.entity.UserRole;

/**
 * Danh tính gắn vào {@code SecurityContext} sau khi token được chấp nhận.
 *
 * <p>Chỉ mang những gì tầng bảo mật cần, không mang cả entity {@code User}:
 * một principal chứa entity JPA rất dễ kéo theo lazy loading ngoài transaction
 * ở những chỗ không ai ngờ tới.
 */
public record AuthenticatedUser(Long id, String email, UserRole role, boolean mustChangePassword) {}
