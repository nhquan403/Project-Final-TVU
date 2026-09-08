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

/** Tin tức và bài khuyến mãi trên landing page. */
@Entity
@Table(name = "posts")
@Getter
@Setter
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String excerpt;

    /** Đã sanitize trước khi lưu. */
    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
}
