package com.tvh.homestay.cms.entity;

import com.tvh.homestay.user.entity.User;
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
import org.springframework.data.annotation.LastModifiedDate;
import jakarta.persistence.EntityListeners;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Một khối nội dung trên landing page, tra theo {@code sectionKey}. */
@Entity
@Table(name = "site_contents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class SiteContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Column(length = 255)
    private String title;

    @Column(length = 255)
    private String subtitle;

    /** Đã sanitize TRƯỚC KHI LƯU — mỗi nơi hiển thị là một chỗ có thể quên. */
    @Column(columnDefinition = "text")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String data;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
