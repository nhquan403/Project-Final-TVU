package com.tvh.homestay.content;

import com.tvh.homestay.content.dto.ContentDtos.BannerView;
import com.tvh.homestay.content.dto.ContentDtos.GalleryImageView;
import com.tvh.homestay.content.dto.ContentDtos.HomeContent;
import com.tvh.homestay.content.dto.ContentDtos.PostDetail;
import com.tvh.homestay.content.dto.ContentDtos.PostSummary;
import com.tvh.homestay.content.dto.ContentDtos.SectionView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nội dung cho trang công khai.
 *
 * <p>Không endpoint nào ở đây trả về bản nháp: bài viết chưa xuất bản, băng-rôn
 * đã tắt hoặc hết hạn, ảnh đã ẩn đều bị lọc ở tầng truy vấn. Lọc ở frontend là
 * gửi dữ liệu chưa công bố ra trình duyệt rồi mới giấu nó đi — ai mở tab Network
 * cũng đọc được.
 */
@RestController
public class PublicContentController {

    private final ContentService content;

    public PublicContentController(ContentService content) {
        this.content = content;
    }

    /** Gói dữ liệu trang chủ trong MỘT lượt gọi. */
    @GetMapping("/api/content/home")
    public HomeContent home() {
        return content.home();
    }

    @GetMapping("/api/content/sections")
    public List<SectionView> sections() {
        return content.publicSections();
    }

    @GetMapping("/api/content/banners")
    public List<BannerView> banners() {
        return content.publicBanners();
    }

    @GetMapping("/api/content/gallery")
    public List<GalleryImageView> gallery() {
        return content.publicGallery();
    }

    @GetMapping("/api/posts")
    public List<PostSummary> posts() {
        return content.publicPosts();
    }

    @GetMapping("/api/posts/{slug}")
    public PostDetail post(@PathVariable String slug) {
        return content.publicPost(slug);
    }
}
