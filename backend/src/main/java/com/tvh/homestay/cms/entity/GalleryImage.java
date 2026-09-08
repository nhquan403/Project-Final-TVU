package com.tvh.homestay.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gallery_images")
@Getter
@Setter
public class GalleryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "public_id", length = 255)
    private String publicId;

    @Column(length = 255)
    private String caption;

    @Column(length = 50)
    private String category;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;
}
