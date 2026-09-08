package com.tvh.homestay.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "room_type_images")
@Getter
@Setter
public class RoomTypeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(nullable = false, length = 500)
    private String url;

    /** id của Cloudinary; NULL khi ảnh lưu cục bộ. Cần để xoá được ảnh trên cloud. */
    @Column(name = "public_id", length = 255)
    private String publicId;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /** Mỗi loại phòng nhiều nhất một ảnh bìa — có UNIQUE index từng phần ép. */
    @Column(name = "is_cover", nullable = false)
    private boolean cover = false;
}
