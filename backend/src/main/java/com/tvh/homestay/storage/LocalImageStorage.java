package com.tvh.homestay.storage;

import com.tvh.homestay.admin.exception.AdminExceptions.ImageStorageFailed;
import com.tvh.homestay.storage.ImageValidator.ValidatedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lưu ảnh xuống đĩa của máy chủ, phục vụ qua {@code /uploads/**}.
 *
 * <p><b>Tên tệp do hệ thống sinh, không bao giờ lấy từ client.</b> Tên do client
 * gửi có thể là {@code ../../application.yml} hoặc {@code anh.jpg.jsp}; dùng nó
 * là mở đường ghi đè tệp ngoài thư mục ảnh. UUID vừa loại bỏ hẳn lớp lỗi đó,
 * vừa khiến không ai đoán được đường dẫn ảnh của người khác.
 *
 * <p>Đuôi tệp lấy từ định dạng mà {@code ImageValidator} vừa GHI RA, không phải
 * từ đuôi tệp gốc.
 */
public class LocalImageStorage implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

    /** Tiền tố URL công khai. Phải khớp {@link UploadedImageController}. */
    static final String URL_PREFIX = "/uploads/";

    private final Path root;

    public LocalImageStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public StoredImage store(ValidatedImage image, String folder) {
        LocalDate today = LocalDate.now();
        String relative = "%s/%04d/%02d/%s.%s".formatted(
                sanitizeFolder(folder),
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                image.extension());

        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            // Không thể xảy ra với UUID và folder đã lọc, nhưng đây là loại lỗi
            // mà "không thể xảy ra" không đủ để bỏ kiểm.
            throw new ImageStorageFailed("Đường dẫn ảnh nằm ngoài thư mục lưu trữ.");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, image.content());
        } catch (IOException e) {
            throw new ImageStorageFailed("Không ghi được ảnh xuống đĩa: " + e.getMessage());
        }
        log.debug("Đã lưu ảnh cục bộ tại {}", target);
        return new StoredImage(URL_PREFIX + relative, null);
    }

    @Override
    public void delete(String url, String publicId) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        Path target = root.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Xoá hỏng không được phép làm hỏng thao tác nghiệp vụ đang chạy:
            // một tệp mồ côi trên đĩa nhẹ hơn nhiều so với một bản ghi không xoá
            // được vì tệp của nó đã biến mất từ trước.
            log.warn("Không xoá được ảnh {}: {}", url, e.getMessage());
        }
    }

    @Override
    public String describe() {
        return "LocalImageStorage (" + root + ")";
    }

    Path getRoot() {
        return root;
    }

    /** Chỉ nhận chữ, số, gạch ngang và gạch dưới — mọi thứ khác bị bỏ. */
    private static String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "misc";
        }
        String cleaned = folder.replaceAll("[^A-Za-z0-9_-]", "");
        return cleaned.isBlank() ? "misc" : cleaned;
    }
}
