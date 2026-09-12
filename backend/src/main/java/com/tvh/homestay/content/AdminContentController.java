package com.tvh.homestay.content;

import com.tvh.homestay.admin.CurrentAdmin;
import com.tvh.homestay.auth.AuthenticatedUser;
import com.tvh.homestay.content.dto.ContentDtos.BannerRequest;
import com.tvh.homestay.content.dto.ContentDtos.BannerView;
import com.tvh.homestay.content.dto.ContentDtos.GalleryImageRequest;
import com.tvh.homestay.content.dto.ContentDtos.GalleryImageView;
import com.tvh.homestay.content.dto.ContentDtos.PostDetail;
import com.tvh.homestay.content.dto.ContentDtos.PostRequest;
import com.tvh.homestay.content.dto.ContentDtos.PostSummary;
import com.tvh.homestay.content.dto.ContentDtos.SectionRequest;
import com.tvh.homestay.content.dto.ContentDtos.SectionView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bốn nhóm nội dung do quản trị viên soạn.
 *
 * <p>Phân quyền do một dòng {@code /api/admin/** → hasRole("ADMIN")} trong
 * {@code SecurityConfig} lo; không lớp nào ở đây tự kiểm lại. Nhưng nhóm đường
 * dẫn {@code /api/admin/content} VẪN phải được khai trong ma trận của
 * {@code EndpointAuthorizationIT} — Phase 7 đã bỏ dòng bao, nên quên khai là
 * test đỏ, đúng như thiết kế.
 */
@RestController
public class AdminContentController {

    private final ContentService content;

    public AdminContentController(ContentService content) {
        this.content = content;
    }

    // ─── Khối nội dung trang ─────────────────────────────────────────────

    @GetMapping("/api/admin/content/sections")
    public List<SectionView> sections() {
        return content.allSections();
    }

    @PutMapping("/api/admin/content/sections/{key}")
    public SectionView saveSection(
            @PathVariable String key,
            @Valid @RequestBody SectionRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return content.saveSection(key, request, CurrentAdmin.idOf(principal));
    }

    // ─── Băng-rôn ────────────────────────────────────────────────────────

    @GetMapping("/api/admin/content/banners")
    public List<BannerView> banners() {
        return content.allBanners();
    }

    @PostMapping("/api/admin/content/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public BannerView createBanner(@Valid @RequestBody BannerRequest request) {
        return content.saveBanner(null, request);
    }

    @PutMapping("/api/admin/content/banners/{id}")
    public BannerView updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        return content.saveBanner(id, request);
    }

    @DeleteMapping("/api/admin/content/banners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(@PathVariable Long id) {
        content.deleteBanner(id);
    }

    // ─── Thư viện ảnh ────────────────────────────────────────────────────

    @GetMapping("/api/admin/content/gallery")
    public List<GalleryImageView> gallery() {
        return content.allGallery();
    }

    @PostMapping("/api/admin/content/gallery")
    @ResponseStatus(HttpStatus.CREATED)
    public GalleryImageView createGalleryImage(@Valid @RequestBody GalleryImageRequest request) {
        return content.saveGalleryImage(null, request);
    }

    @PutMapping("/api/admin/content/gallery/{id}")
    public GalleryImageView updateGalleryImage(
            @PathVariable Long id, @Valid @RequestBody GalleryImageRequest request) {
        return content.saveGalleryImage(id, request);
    }

    @DeleteMapping("/api/admin/content/gallery/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGalleryImage(@PathVariable Long id) {
        content.deleteGalleryImage(id);
    }

    // ─── Tin tức ─────────────────────────────────────────────────────────

    @GetMapping("/api/admin/content/posts")
    public List<PostSummary> posts() {
        return content.allPosts();
    }

    @GetMapping("/api/admin/content/posts/{id}")
    public PostDetail post(@PathVariable Long id) {
        return content.adminPost(id);
    }

    @PostMapping("/api/admin/content/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDetail createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return content.savePost(null, request, CurrentAdmin.idOf(principal));
    }

    @PutMapping("/api/admin/content/posts/{id}")
    public PostDetail updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return content.savePost(id, request, CurrentAdmin.idOf(principal));
    }

    @DeleteMapping("/api/admin/content/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id) {
        content.deletePost(id);
    }
}
