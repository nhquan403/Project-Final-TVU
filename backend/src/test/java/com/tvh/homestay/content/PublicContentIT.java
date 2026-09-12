package com.tvh.homestay.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.schema.AbstractPostgresIT;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Nội dung công khai: trang chủ phải đứng vững khi CMS trống, và bản nháp không
 * bao giờ ra khỏi máy chủ.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "booking.expiry-scan-ms=3600000")
class PublicContentIT extends AbstractPostgresIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanSlate() {
        jdbc.execute("DELETE FROM posts");
        jdbc.execute("DELETE FROM banners");
        jdbc.execute("DELETE FROM gallery_images");
        jdbc.execute("DELETE FROM site_contents");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("CMS còn TRỐNG: /api/content/sections vẫn trả 200 kèm nội dung mặc định")
    void emptyCmsStillServesDefaults() {
        ResponseEntity<List> response = rest.getForEntity(url("/api/content/sections"), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Không có mặc định thì trang chủ hiện ra ba mảng trắng, và người xem
        // không phân biệt được "chưa nhập nội dung" với "trang hỏng".
        assertThat(response.getBody()).isNotEmpty();
        assertThat(keys(response.getBody())).contains("hero", "about", "contact");
    }

    @Test
    @DisplayName("Nội dung thật ĐÈ LÊN mặc định, khối chưa nhập vẫn giữ mặc định")
    void savedContentOverridesDefaults() {
        jdbc.update("""
                INSERT INTO site_contents (section_key, title, subtitle, body)
                VALUES ('hero', 'Tiêu đề thật', 'Phụ đề thật', '<p>Nội dung thật</p>')
                """);

        ResponseEntity<List> response = rest.getForEntity(url("/api/content/sections"), List.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> hero = ((List<Map<String, Object>>) response.getBody()).stream()
                .filter(section -> "hero".equals(section.get("key")))
                .findFirst()
                .orElseThrow();
        assertThat(hero.get("title")).isEqualTo("Tiêu đề thật");
        assertThat(keys(response.getBody())).contains("about", "contact");
    }

    @Test
    @DisplayName("/api/posts chỉ trả bài ĐÃ xuất bản — bản nháp không rời máy chủ")
    void draftPostsNeverLeaveTheServer() {
        jdbc.update("""
                INSERT INTO posts (slug, title, excerpt, content, published, published_at)
                VALUES ('da-xuat-ban', 'Đã xuất bản', 'tóm tắt', '<p>nội dung</p>', true, now()),
                       ('ban-nhap', 'Bản nháp', 'tóm tắt', '<p>chưa xong</p>', false, null)
                """);

        ResponseEntity<List> response = rest.getForEntity(url("/api/posts"), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        // Lọc ở TRUY VẤN, không ở frontend: lọc ở frontend là gửi bản nháp ra
        // trình duyệt rồi mới giấu đi, và ai mở tab Network cũng đọc được.
        assertThat(response.getBody().toString()).doesNotContain("Bản nháp");
    }

    @Test
    @DisplayName("Mở bài chưa xuất bản theo slug → 404")
    void draftPostBySlugIsNotFound() {
        jdbc.update("""
                INSERT INTO posts (slug, title, content, published)
                VALUES ('ban-nhap', 'Bản nháp', '<p>chưa xong</p>', false)
                """);

        assertThat(rest.getForEntity(url("/api/posts/ban-nhap"), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Băng-rôn đã tắt hoặc hết hạn không hiện ra trang công khai")
    void expiredAndInactiveBannersAreHidden() {
        jdbc.update("""
                INSERT INTO banners (title, image_url, active, starts_at, ends_at) VALUES
                  ('Đang chạy', '/uploads/a.png', true,  now() - interval '1 day', now() + interval '1 day'),
                  ('Đã tắt',    '/uploads/b.png', false, null, null),
                  ('Hết hạn',   '/uploads/c.png', true,  now() - interval '10 day', now() - interval '1 day'),
                  ('Chưa tới',  '/uploads/d.png', true,  now() + interval '1 day', null)
                """);

        ResponseEntity<List> response = rest.getForEntity(url("/api/content/banners"), List.class);

        assertThat(response.getBody()).hasSize(1);
        // Một chiến dịch đã hết hạn mà vẫn nằm trên trang chủ là lời hứa ưu đãi
        // không còn giá trị — khách đọc rồi vào đặt phòng sẽ thấy giá khác.
        assertThat(response.getBody().toString()).contains("Đang chạy").doesNotContain("Hết hạn");
    }

    @Test
    @DisplayName("Ảnh thư viện đã ẩn không ra trang công khai")
    void inactiveGalleryImagesAreHidden() {
        jdbc.update("""
                INSERT INTO gallery_images (url, caption, active) VALUES
                  ('/uploads/a.png', 'Hiện', true),
                  ('/uploads/b.png', 'Ẩn',   false)
                """);

        ResponseEntity<List> response = rest.getForEntity(url("/api/content/gallery"), List.class);

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("/api/content/home gói cả bốn khối trong MỘT lượt gọi")
    void homeBundlesEverythingInOneCall() {
        ResponseEntity<Map> response = rest.getForEntity(url("/api/content/home"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("sections", "banners", "gallery", "latestPosts");
    }

    @SuppressWarnings("unchecked")
    private static List<String> keys(List<?> sections) {
        return ((List<Map<String, Object>>) sections).stream()
                .map(section -> String.valueOf(section.get("key")))
                .toList();
    }
}
