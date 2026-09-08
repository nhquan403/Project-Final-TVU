package com.tvh.homestay.user.entity;

import com.tvh.homestay.common.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Luôn lưu chữ thường — có CHECK ở tầng cơ sở dữ liệu ép điều đó. */
    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    /**
     * Tăng lên khi logout, khoá tài khoản hoặc đổi quyền. JWT mang giá trị này
     * lúc phát hành; lệch số là token bị từ chối ngay lập tức, không phải chờ
     * hết hạn. Đây là cách thu hồi token mà không cần kho phiên phía máy chủ.
     */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;
}
