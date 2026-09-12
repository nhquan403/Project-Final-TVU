package com.tvh.homestay.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Lọc HTML và kiểm scheme URL — hai lớp phòng thủ ở TẦNG VÀO.
 *
 * <p>Không cần Spring: đây là hai lớp thuần, và phần đáng kiểm là chuỗi vào ra.
 */
class ContentSanitizerTest {

    private final HtmlSanitizer sanitizer =
            new HtmlSanitizer(List.of("/uploads/", "https://res.cloudinary.com/"));
    private final UrlSchemeValidator urls = new UrlSchemeValidator();

    @Test
    @DisplayName("Thẻ script bị loại, chữ bên trong không trở thành nội dung thực thi được")
    void scriptIsRemoved() {
        String clean = sanitizer.sanitize(
                "<p>Xin chào</p><script>fetch('/api/admin/bookings')</script>");

        assertThat(clean).contains("<p>Xin chào</p>");
        assertThat(clean).doesNotContain("<script");
        assertThat(clean).doesNotContain("fetch(");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<a href=\"javascript:alert(1)\">bấm đi</a>",
        "<a href=\"JaVaScRiPt:alert(1)\">bấm đi</a>",
        "<a href=\"data:text/html;base64,PHNjcmlwdD4=\">bấm đi</a>",
    })
    @DisplayName("Link mang scheme nguy hiểm mất href, dù viết hoa hay đổi scheme")
    void dangerousLinkSchemesLoseTheirHref(String html) {
        String clean = sanitizer.sanitize(html);

        assertThat(clean).doesNotContain("javascript:");
        assertThat(clean).doesNotContain("data:");
        assertThat(clean).as("chữ vẫn còn, chỉ mất khả năng bấm").contains("bấm đi");
    }

    @Test
    @DisplayName("Link http/https giữ nguyên và được gắn rel=nofollow")
    void ordinaryLinksSurvive() {
        String clean = sanitizer.sanitize("<a href=\"https://tvu.edu.vn\">Trường</a>");

        assertThat(clean).contains("href=\"https://tvu.edu.vn\"");
        assertThat(clean).contains("nofollow");
    }

    @Test
    @DisplayName("Ảnh ngoài danh sách origin mất src — chặn kênh theo dõi qua ảnh")
    void foreignImageSourcesAreDropped() {
        String clean = sanitizer.sanitize("<img src=\"https://evil.example/track.png\" alt=\"x\">");

        assertThat(clean)
                .as("mỗi lần trang hiện ra là một request kèm IP và Referer gửi tới máy chủ lạ")
                .doesNotContain("evil.example");
    }

    @Test
    @DisplayName("Ảnh từ /uploads và Cloudinary được giữ")
    void ownImageSourcesSurvive() {
        assertThat(sanitizer.sanitize("<img src=\"/uploads/misc/2026/09/a.png\" alt=\"phòng\">"))
                .contains("/uploads/misc/2026/09/a.png");
        assertThat(sanitizer.sanitize(
                        "<img src=\"https://res.cloudinary.com/demo/image/upload/a.jpg\" alt=\"phòng\">"))
                .contains("res.cloudinary.com");
    }

    @Test
    @DisplayName("Thẻ trong danh sách trắng đi qua nguyên vẹn")
    void allowedTagsSurvive() {
        String clean = sanitizer.sanitize(
                "<h2>Tiêu đề</h2><p><strong>đậm</strong> và <em>nghiêng</em></p>"
                        + "<ul><li>một</li><li>hai</li></ul><blockquote>trích</blockquote>");

        assertThat(clean).contains("<h2>", "<strong>", "<em>", "<ul>", "<li>", "<blockquote>");
    }

    @Test
    @DisplayName("Thẻ ngoài danh sách trắng bị loại, kể cả thẻ không ai nghĩ tới")
    void tagsOutsideTheAllowlistAreRemoved() {
        String clean = sanitizer.sanitize(
                "<iframe src=\"https://evil.example\"></iframe>"
                        + "<svg onload=\"alert(1)\"></svg>"
                        + "<p onmouseover=\"alert(1)\">chữ</p>");

        assertThat(clean).doesNotContain("<iframe", "<svg", "onload", "onmouseover");
        assertThat(clean).contains("chữ");
    }

    @Test
    @DisplayName("null vào thì null ra, không ném ngoại lệ")
    void nullIsPassedThrough() {
        assertThat(sanitizer.sanitize(null)).isNull();
        assertThat(urls.validate(null)).isNull();
        assertThat(urls.validate("  ")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:text/html,<script>", "vbscript:msgbox"})
    @DisplayName("Ô URL trần chỉ nhận http/https/mailto — danh sách trắng, không phải danh sách đen")
    void bannerLinkRejectsDangerousSchemes(String url) {
        assertThatThrownBy(() -> urls.validate(url))
                .isInstanceOf(InvalidAdminRequest.class)
                .hasMessageContaining("Chỉ chấp nhận");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://homestaytvh.vn/khuyen-mai",
        "http://homestaytvh.vn",
        "mailto:lienhe@homestaytvh.vn",
        "/tin-tuc/uu-dai-he",
    })
    @DisplayName("Đường dẫn hợp lệ và đường dẫn tương đối đi qua")
    void validUrlsSurvive(String url) {
        assertThat(urls.validate(url)).isEqualTo(url);
    }
}
