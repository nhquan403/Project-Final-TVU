package com.tvh.homestay.content;

import java.util.List;
import java.util.Locale;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lọc HTML của nội dung giàu định dạng, Ở TẦNG VÀO — trước khi lưu.
 *
 * <h2>Vì sao lọc lúc GHI chứ không lúc ĐỌC</h2>
 *
 * <p>Một mẩu nội dung được đọc ở nhiều nơi: trang chủ, trang bài viết, màn hình
 * xem trước của admin, và cả email về sau. Lọc lúc đọc nghĩa là mỗi nơi đó là
 * một chỗ có thể quên, và chỗ quên đầu tiên là chỗ bị khai thác. Lọc lúc ghi thì
 * thứ nằm trong cơ sở dữ liệu đã là thứ an toàn — mọi nơi đọc đều an toàn theo.
 *
 * <p>Đổi lại, nội dung gốc mất đi. Đó là đánh đổi có chủ ý: đây là nội dung do
 * quản trị viên soạn, không phải bản ghi pháp lý cần giữ nguyên văn.
 *
 * <h2>Danh sách TRẮNG tường minh</h2>
 *
 * <p>Chỉ những thẻ dưới đây đi qua. Danh sách đen luôn thiếu: bỏ {@code script}
 * thì còn {@code svg}, {@code iframe}, {@code object}, thuộc tính {@code on*},
 * và cả những thẻ HTML chưa tồn tại lúc viết dòng này.
 *
 * <h2>Ảnh chỉ từ nơi mình kiểm soát</h2>
 *
 * <p>{@code img[src]} trỏ ra ngoài không chỉ là chuyện thẩm mỹ: mỗi lần trang
 * hiển thị, trình duyệt của khách gửi một request tới máy chủ lạ kèm IP và
 * header {@code Referer} chứa đường dẫn đang xem. Đó là kênh theo dõi mà không
 * ai đồng ý, và cũng là kênh xác nhận "đã có người mở nội dung này".
 */
@Component
public class HtmlSanitizer {

    private final PolicyFactory policy;
    private final List<String> allowedImagePrefixes;

    public HtmlSanitizer(
            @Value("${content.image-src-allowlist:/uploads/,https://res.cloudinary.com/}")
            List<String> allowedImagePrefixes) {
        this.allowedImagePrefixes = allowedImagePrefixes.stream()
                .map(prefix -> prefix.trim().toLowerCase(Locale.ROOT))
                .filter(prefix -> !prefix.isEmpty())
                .toList();

        this.policy = new HtmlPolicyBuilder()
                .allowElements(
                        "p", "h2", "h3", "h4", "ul", "ol", "li",
                        "strong", "em", "a", "img", "br", "blockquote")
                // Chỉ ba scheme này. `javascript:` và `data:` bị loại cùng với
                // mọi scheme khác chưa nghĩ tới.
                .allowUrlProtocols("http", "https", "mailto")
                .allowAttributes("href").onElements("a")
                // Link ra ngoài mở tab mới và không mang PageRank sang: đây là
                // nội dung do người soạn, không phải đề cử của trang.
                .requireRelNofollowOnLinks()
                .allowAttributes("alt").onElements("img")
                .allowAttributes("src")
                .matching((elementName, attributeName, value) -> allowedImageSource(value))
                .onElements("img")
                .toFactory();
    }

    /** @return HTML đã lọc; {@code null} vào thì {@code null} ra. */
    public String sanitize(String html) {
        return html == null ? null : policy.sanitize(html);
    }

    /*
     * KHÔNG có hàm "làm sạch" cho trường văn bản thuần, và đó là chủ ý.
     *
     * Tiêu đề băng-rôn, chú thích ảnh, tiêu đề và nội dung đánh giá được lưu
     * ĐÚNG NGUYÊN VĂN người dùng gõ. Chạy chúng qua bộ lọc HTML sẽ escape dấu
     * `&` thành `&amp;`, rồi text binding của Angular hiển thị ra đúng chữ
     * `&amp;` — một lỗi hiện ngay trên màn hình khách, đổi lấy một mối nguy
     * không tồn tại.
     *
     * Chúng an toàn vì nơi hiển thị KHÔNG BAO GIỜ diễn giải chúng thành HTML:
     * `{{ }}` của Angular tự escape lúc render. Lớp phòng thủ nằm ở chỗ đọc,
     * không phải ở chỗ ghi — ngược hẳn với nội dung giàu định dạng phía trên,
     * và sự khác biệt đó là có lý do: nội dung giàu định dạng BẮT BUỘC phải
     * được diễn giải thành HTML, nên nó không có lớp phòng thủ lúc đọc.
     */

    /** @return giá trị {@code src} nếu thuộc nơi mình kiểm soát, {@code null} để loại thuộc tính */
    private String allowedImageSource(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return allowedImagePrefixes.stream().anyMatch(normalized::startsWith) ? value.trim() : null;
    }
}
