package com.tvh.homestay.storage;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phục vụ ảnh đã tải lên khi đang dùng {@link LocalImageStorage}.
 *
 * <p><b>Vì sao là controller chứ không phải một resource handler.</b> Ba header
 * dưới đây là phần quan trọng nhất của endpoint này, và resource handler mặc
 * định không đặt cái nào:
 *
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} — không có nó, trình duyệt tự
 *       đoán kiểu nội dung từ mấy byte đầu và một tệp được đoán thành HTML sẽ
 *       chạy như HTML, cùng origin với ứng dụng.
 *   <li>{@code Content-Type} do MÁY CHỦ quyết định, suy từ đuôi tệp nằm trong
 *       danh sách trắng — đuôi nào không nằm trong đó thì trả 404 thay vì đoán.
 *   <li>{@code Content-Disposition: inline} kèm tên tệp do máy chủ đặt.
 * </ul>
 *
 * <p>Đặt các header này Ở TẦNG ỨNG DỤNG, không đợi nginx của Phase 9: bản chạy
 * tay lúc bảo vệ không có nginx, và đó chính là lúc ảnh được tải lên nhiều nhất.
 */
@RestController
public class UploadedImageController {

    private static final String PREFIX = "/uploads/";

    /** Danh sách trắng đuôi tệp → kiểu nội dung. Khớp đúng thứ ImageValidator ghi ra. */
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");

    private final Path root;

    public UploadedImageController(@Value("${storage.local.dir:uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int prefixAt = uri.indexOf(PREFIX);
        if (prefixAt < 0) {
            throw notFound();
        }
        String relative = URLDecoder.decode(uri.substring(prefixAt + PREFIX.length()),
                StandardCharsets.UTF_8);

        Path target = root.resolve(relative).normalize();
        // Chốt chặn path traversal. `normalize()` một mình là chưa đủ: nó xử lý
        // được `..` nhưng không nói cho ai biết kết quả có còn nằm trong thư
        // mục gốc hay không.
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw notFound();
        }

        String contentType = CONTENT_TYPES.get(extensionOf(target));
        if (contentType == null) {
            // Đuôi lạ nghĩa là tệp này không phải thứ hệ thống ghi ra. Trả 404
            // thay vì đoán kiểu nội dung — đoán là đúng cái việc nosniff sinh ra
            // để chặn.
            throw notFound();
        }

        long length;
        try {
            length = Files.size(target);
        } catch (IOException e) {
            throw notFound();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + target.getFileName() + "\"")
                // Tên tệp là UUID nên nội dung không bao giờ đổi dưới cùng một
                // URL — cache vĩnh viễn là an toàn và đúng.
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentLength(length)
                .body(new FileSystemResource(target));
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh.");
    }

    private static String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
