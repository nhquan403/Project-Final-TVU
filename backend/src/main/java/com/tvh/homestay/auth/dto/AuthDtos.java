package com.tvh.homestay.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO của luồng xác thực.
 *
 * <p>{@link RegisterRequest} cố ý KHÔNG có trường {@code role}. Vai trò do
 * service gán cứng là {@code CUSTOMER}. Để trường đó tồn tại rồi "nhớ bỏ qua"
 * là mời gọi một lần quên duy nhất biến thành leo thang đặc quyền.
 */
public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 150) String fullName,
            @Size(max = 20) String phone) {}

    public record LoginRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 72) String password) {}

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    /**
     * Access token trả trong thân phản hồi để frontend giữ trong bộ nhớ.
     * Refresh token KHÔNG có ở đây — nó đi bằng cookie HttpOnly, chính là để
     * JavaScript không đọc được.
     */
    public record TokenResponse(String accessToken, long expiresInSeconds) {}

    public record MeResponse(
            Long id,
            String email,
            String fullName,
            String phone,
            String role,
            boolean mustChangePassword) {}
}
