package com.tvh.homestay.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "banners")
@Getter
@Setter
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "public_id", length = 255)
    private String publicId;

    /** Scheme http/https kiểm ở tầng ứng dụng: link {@code javascript:} lọt vào đây là XSS ngay trang chủ. */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;
}
