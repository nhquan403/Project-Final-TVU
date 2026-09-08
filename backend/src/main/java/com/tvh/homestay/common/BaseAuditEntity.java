package com.tvh.homestay.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Hai cột thời gian dùng chung cho các bảng có vòng đời dài.
 *
 * <p>Chỉ những bảng thật sự có cả hai cột mới kế thừa lớp này — bảng chỉ có
 * {@code created_at} tự khai báo lấy, vì {@code ddl-auto=validate} sẽ báo lỗi
 * nếu entity khai một cột mà migration không có.
 *
 * <p>Kiểu {@link OffsetDateTime} chứ không phải {@code LocalDateTime}: cột là
 * {@code timestamptz}, và bỏ mất phần lệch múi giờ ở tầng Java là cách chắc
 * chắn nhất để báo cáo doanh thu lệch một ngày ở khung giờ nửa đêm.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
