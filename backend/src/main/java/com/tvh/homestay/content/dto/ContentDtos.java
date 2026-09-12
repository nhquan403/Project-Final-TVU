package com.tvh.homestay.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO của lớp nội dung (CMS) — cả phần công khai lẫn phần quản trị. */
public final class ContentDtos {

    private ContentDtos() {}

    /**
     * Một khối nội dung của trang.
     *
     * <p>{@code body} là HTML ĐÃ được lọc ở tầng vào, nên nơi hiển thị dùng
     * {@code [innerHTML]} được. {@code title} và {@code subtitle} là văn bản
     * thuần, hiển thị bằng text binding.
     */
    public record SectionView(
            String key,
            String title,
            String subtitle,
            String body,
            String imageUrl,
            OffsetDateTime updatedAt) {}

    public record SectionRequest(
            @Size(max = 255) String title,
            @Size(max = 255) String subtitle,
            String body,
            @Size(max = 500) String imageUrl) {}

    public record BannerView(
            Long id,
            String title,
            String imageUrl,
            String publicId,
            String linkUrl,
            int displayOrder,
            boolean active,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {}

    public record BannerRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 500) String imageUrl,
            @Size(max = 255) String publicId,
            @Size(max = 500) String linkUrl,
            int displayOrder,
            boolean active,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {}

    public record GalleryImageView(
            Long id,
            String url,
            String publicId,
            String caption,
            String category,
            int displayOrder,
            boolean active) {}

    public record GalleryImageRequest(
            @NotBlank @Size(max = 500) String url,
            @Size(max = 255) String publicId,
            @Size(max = 255) String caption,
            @Size(max = 50) String category,
            int displayOrder,
            boolean active) {}

    /** Bài viết ở danh sách: KHÔNG kèm {@code content} để trang danh sách không tải thừa. */
    public record PostSummary(
            Long id,
            String slug,
            String title,
            String excerpt,
            String coverImageUrl,
            boolean published,
            OffsetDateTime publishedAt) {}

    /** {@code content} là HTML đã lọc ở tầng vào. */
    public record PostDetail(
            Long id,
            String slug,
            String title,
            String excerpt,
            String content,
            String coverImageUrl,
            boolean published,
            OffsetDateTime publishedAt) {}

    public record PostRequest(
            @NotBlank @Size(max = 200) String slug,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 4000) String excerpt,
            String content,
            @Size(max = 500) String coverImageUrl,
            boolean published) {}

    /** Gói dữ liệu trang chủ trong MỘT lượt gọi, để trang không nhấp nháy từng khối. */
    public record HomeContent(
            List<SectionView> sections,
            List<BannerView> banners,
            List<GalleryImageView> gallery,
            List<PostSummary> latestPosts) {}
}
