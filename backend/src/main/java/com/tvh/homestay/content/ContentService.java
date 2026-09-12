package com.tvh.homestay.content;

import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.cms.entity.Banner;
import com.tvh.homestay.cms.entity.GalleryImage;
import com.tvh.homestay.cms.entity.Post;
import com.tvh.homestay.cms.entity.SiteContent;
import com.tvh.homestay.cms.repository.BannerRepository;
import com.tvh.homestay.cms.repository.GalleryImageRepository;
import com.tvh.homestay.cms.repository.PostRepository;
import com.tvh.homestay.cms.repository.SiteContentRepository;
import com.tvh.homestay.content.dto.ContentDtos.BannerRequest;
import com.tvh.homestay.content.dto.ContentDtos.BannerView;
import com.tvh.homestay.content.dto.ContentDtos.GalleryImageRequest;
import com.tvh.homestay.content.dto.ContentDtos.GalleryImageView;
import com.tvh.homestay.content.dto.ContentDtos.HomeContent;
import com.tvh.homestay.content.dto.ContentDtos.PostDetail;
import com.tvh.homestay.content.dto.ContentDtos.PostRequest;
import com.tvh.homestay.content.dto.ContentDtos.PostSummary;
import com.tvh.homestay.content.dto.ContentDtos.SectionRequest;
import com.tvh.homestay.content.dto.ContentDtos.SectionView;
import com.tvh.homestay.user.entity.User;
import com.tvh.homestay.user.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc và ghi nội dung của trang.
 *
 * <p>Mọi đường GHI đi qua {@link HtmlSanitizer} và {@link UrlSchemeValidator}
 * TRƯỚC khi chạm cơ sở dữ liệu. Không có đường ghi nào khác — đó là điều kiện
 * để nơi đọc không phải lọc lại, và để không có nơi đọc nào lỡ quên.
 */
@Service
public class ContentService {

    /** Số bài mới nhất nhúng vào gói dữ liệu trang chủ. */
    private static final int HOME_POST_LIMIT = 3;

    private final SiteContentRepository sections;
    private final BannerRepository banners;
    private final GalleryImageRepository gallery;
    private final PostRepository posts;
    private final UserRepository users;
    private final HtmlSanitizer sanitizer;
    private final UrlSchemeValidator urls;
    private final Clock clock;

    public ContentService(
            SiteContentRepository sections,
            BannerRepository banners,
            GalleryImageRepository gallery,
            PostRepository posts,
            UserRepository users,
            HtmlSanitizer sanitizer,
            UrlSchemeValidator urls,
            Clock clock) {
        this.sections = sections;
        this.banners = banners;
        this.gallery = gallery;
        this.posts = posts;
        this.users = users;
        this.sanitizer = sanitizer;
        this.urls = urls;
        this.clock = clock;
    }

    // ─── Công khai ───────────────────────────────────────────────────────

    /**
     * Toàn bộ khối nội dung, dữ liệu thật ĐÈ LÊN mặc định.
     *
     * <p>Khối chưa ai nhập vẫn trả về nội dung mặc định, nên trang chủ không bao
     * giờ hiện ra một mảng trắng không giải thích được.
     */
    @Transactional(readOnly = true)
    public List<SectionView> publicSections() {
        Map<String, SectionView> merged = ContentDefaults.all();
        sections.findAllByOrderBySectionKeyAsc()
                .forEach(row -> merged.put(row.getSectionKey(), toView(row)));
        return List.copyOf(merged.values());
    }

    @Transactional(readOnly = true)
    public List<BannerView> publicBanners() {
        return banners.findVisible(OffsetDateTime.now(clock)).stream()
                .map(ContentService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GalleryImageView> publicGallery() {
        return gallery.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(ContentService::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostSummary> publicPosts() {
        return posts.findByPublishedTrueOrderByPublishedAtDescIdDesc().stream()
                .map(ContentService::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostDetail publicPost(String slug) {
        return posts.findBySlugIgnoreCaseAndPublishedTrue(slug)
                .map(ContentService::toDetail)
                .orElseThrow(() -> new AdminResourceNotFound("bài viết " + slug));
    }

    /** Gói dữ liệu trang chủ trong một lượt gọi, để trang không nhấp nháy từng khối. */
    @Transactional(readOnly = true)
    public HomeContent home() {
        return new HomeContent(
                publicSections(),
                publicBanners(),
                publicGallery(),
                posts.findByPublishedTrueOrderByPublishedAtDescIdDesc(Limit.of(HOME_POST_LIMIT))
                        .stream()
                        .map(ContentService::toSummary)
                        .toList());
    }

    // ─── Quản trị ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SectionView> allSections() {
        return publicSections();
    }

    @Transactional
    public SectionView saveSection(String key, SectionRequest request, Long adminId) {
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        SiteContent row = sections.findBySectionKey(normalized).orElseGet(SiteContent::new);
        row.setSectionKey(normalized);
        row.setTitle(request.title());
        row.setSubtitle(request.subtitle());
        // Đây là nội dung giàu định dạng: lọc NGAY tại đây, không đợi lúc đọc.
        row.setBody(sanitizer.sanitize(request.body()));
        row.setImageUrl(urls.validate(request.imageUrl()));
        row.setUpdatedBy(admin(adminId));
        return toView(sections.save(row));
    }

    @Transactional(readOnly = true)
    public List<BannerView> allBanners() {
        return banners.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(ContentService::toView)
                .toList();
    }

    @Transactional
    public BannerView saveBanner(Long id, BannerRequest request) {
        Banner banner = id == null
                ? new Banner()
                : banners.findById(id).orElseThrow(() -> new AdminResourceNotFound("băng-rôn #" + id));
        // Tiêu đề lưu NGUYÊN VĂN, hiển thị bằng text binding — xem javadoc
        // HtmlSanitizer về việc vì sao không escape ở đây.
        banner.setTitle(request.title().trim());
        banner.setImageUrl(urls.validate(request.imageUrl()));
        banner.setPublicId(request.publicId());
        banner.setLinkUrl(urls.validate(request.linkUrl()));
        banner.setDisplayOrder(request.displayOrder());
        banner.setActive(request.active());
        banner.setStartsAt(request.startsAt());
        banner.setEndsAt(request.endsAt());
        if (banner.getStartsAt() != null && banner.getEndsAt() != null
                && !banner.getEndsAt().isAfter(banner.getStartsAt())) {
            throw new InvalidAdminRequest("Thời điểm kết thúc phải sau thời điểm bắt đầu.");
        }
        return toView(banners.save(banner));
    }

    @Transactional
    public void deleteBanner(Long id) {
        banners.delete(banners.findById(id)
                .orElseThrow(() -> new AdminResourceNotFound("băng-rôn #" + id)));
    }

    @Transactional(readOnly = true)
    public List<GalleryImageView> allGallery() {
        return gallery.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(ContentService::toView)
                .toList();
    }

    @Transactional
    public GalleryImageView saveGalleryImage(Long id, GalleryImageRequest request) {
        GalleryImage image = id == null
                ? new GalleryImage()
                : gallery.findById(id).orElseThrow(() -> new AdminResourceNotFound("ảnh #" + id));
        image.setUrl(urls.validate(request.url()));
        image.setPublicId(request.publicId());
        image.setCaption(request.caption());
        image.setCategory(request.category());
        image.setDisplayOrder(request.displayOrder());
        image.setActive(request.active());
        return toView(gallery.save(image));
    }

    @Transactional
    public void deleteGalleryImage(Long id) {
        gallery.delete(gallery.findById(id)
                .orElseThrow(() -> new AdminResourceNotFound("ảnh #" + id)));
    }

    @Transactional(readOnly = true)
    public List<PostSummary> allPosts() {
        return posts.findAllByOrderByIdDesc().stream().map(ContentService::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public PostDetail adminPost(Long id) {
        return posts.findById(id).map(ContentService::toDetail)
                .orElseThrow(() -> new AdminResourceNotFound("bài viết #" + id));
    }

    @Transactional
    public PostDetail savePost(Long id, PostRequest request, Long adminId) {
        Post post = id == null
                ? new Post()
                : posts.findById(id).orElseThrow(() -> new AdminResourceNotFound("bài viết #" + id));

        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        posts.findBySlugIgnoreCase(slug).ifPresent(other -> {
            if (!other.getId().equals(post.getId())) {
                throw new InvalidAdminRequest("Đường dẫn " + slug + " đã được dùng cho bài khác.");
            }
        });

        post.setSlug(slug);
        post.setTitle(request.title().trim());
        post.setExcerpt(request.excerpt());
        post.setContent(sanitizer.sanitize(request.content()));
        post.setCoverImageUrl(urls.validate(request.coverImageUrl()));
        // publishedAt đóng mốc ở lần xuất bản ĐẦU TIÊN. Đặt lại mỗi lần sửa sẽ
        // đẩy bài cũ lên đầu danh sách chỉ vì ai đó sửa một lỗi chính tả.
        if (request.published() && !post.isPublished()) {
            post.setPublishedAt(OffsetDateTime.now(clock));
        }
        post.setPublished(request.published());
        if (post.getAuthor() == null) {
            post.setAuthor(admin(adminId));
        }
        return toDetail(posts.save(post));
    }

    @Transactional
    public void deletePost(Long id) {
        posts.delete(posts.findById(id)
                .orElseThrow(() -> new AdminResourceNotFound("bài viết #" + id)));
    }

    // ─────────────────────────────────────────────────────────────────────

    private User admin(Long adminId) {
        return adminId == null ? null : users.findById(adminId).orElse(null);
    }

    private static SectionView toView(SiteContent row) {
        return new SectionView(
                row.getSectionKey(),
                row.getTitle(),
                row.getSubtitle(),
                row.getBody(),
                row.getImageUrl(),
                row.getUpdatedAt());
    }

    private static BannerView toView(Banner banner) {
        return new BannerView(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getPublicId(),
                banner.getLinkUrl(),
                banner.getDisplayOrder(),
                banner.isActive(),
                banner.getStartsAt(),
                banner.getEndsAt());
    }

    private static GalleryImageView toView(GalleryImage image) {
        return new GalleryImageView(
                image.getId(),
                image.getUrl(),
                image.getPublicId(),
                image.getCaption(),
                image.getCategory(),
                image.getDisplayOrder(),
                image.isActive());
    }

    private static PostSummary toSummary(Post post) {
        return new PostSummary(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getCoverImageUrl(),
                post.isPublished(),
                post.getPublishedAt());
    }

    private static PostDetail toDetail(Post post) {
        return new PostDetail(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getContent(),
                post.getCoverImageUrl(),
                post.isPublished(),
                post.getPublishedAt());
    }
}
