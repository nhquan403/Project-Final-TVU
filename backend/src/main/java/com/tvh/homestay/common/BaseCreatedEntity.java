package com.tvh.homestay.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Chỉ {@code created_at}, cho các bảng chỉ ghi thêm và không bao giờ sửa:
 * nhật ký trạng thái, hộp thư đi, refresh token, đánh giá.
 *
 * <p>Tách khỏi {@link BaseAuditEntity} vì {@code ddl-auto=validate} báo lỗi
 * ngay nếu entity khai một cột mà migration không có — không thể dùng chung
 * một lớp cha cho cả hai nhóm bảng.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
