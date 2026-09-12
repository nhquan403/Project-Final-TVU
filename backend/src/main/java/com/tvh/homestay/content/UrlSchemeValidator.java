package com.tvh.homestay.content;

import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Kiểm scheme của một URL do người dùng nhập vào ô riêng (không phải trong HTML).
 *
 * <p>Dùng cho {@code banners.link_url}. Một băng-rôn có
 * {@code href="javascript:fetch('/api/admin/...')"} là XSS ngay trang chủ, và
 * nó không đi qua bộ lọc HTML vì đây là một cột URL trần, không phải nội dung
 * giàu định dạng.
 *
 * <p><b>Danh sách TRẮNG, không phải danh sách đen.</b> Chặn {@code javascript:}
 * thì còn {@code data:}, {@code vbscript:}, và cả biến thể viết hoa lẫn ký tự
 * điều khiển chen giữa. Liệt kê cái được phép thì mọi thứ chưa nghĩ tới đều bị
 * loại theo mặc định.
 */
@Component
public class UrlSchemeValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "mailto");

    /**
     * @return URL đã chuẩn hoá, hoặc {@code null} khi đầu vào rỗng
     * @throws InvalidAdminRequest khi scheme không nằm trong danh sách trắng
     */
    public String validate(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();

        // Đường dẫn tương đối trong chính trang này (/tin-tuc/abc) là an toàn và
        // không có scheme để kiểm.
        if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
            return trimmed;
        }

        int colon = trimmed.indexOf(':');
        if (colon <= 0) {
            throw new InvalidAdminRequest(
                    "Đường dẫn phải bắt đầu bằng http://, https://, mailto: hoặc dấu /.");
        }
        String scheme = trimmed.substring(0, colon).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new InvalidAdminRequest(
                    "Chỉ chấp nhận đường dẫn http, https hoặc mailto. Đã chặn: " + scheme + ":");
        }
        return trimmed;
    }
}
