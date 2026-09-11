package com.tvh.homestay.storage;

import com.tvh.homestay.admin.exception.AdminExceptions.ImageTooLarge;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.admin.exception.AdminExceptions.UnsupportedImageType;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Kiểm và CHUẨN HOÁ LẠI mọi ảnh trước khi nó chạm tới nơi lưu trữ.
 *
 * <h2>Vì sao SVG bị loại</h2>
 *
 * <p>SVG là XML thuần. Nó không có magic bytes cố định, nên mọi bộ dò nội dung
 * đều trả về {@code image/svg+xml} và kết luận "ảnh hợp lệ". Nhưng khi được
 * phục vụ CÙNG ORIGIN với ứng dụng, trình duyệt thực thi {@code <script>} bên
 * trong nó — tức là một ảnh tải lên trở thành XSS lưu trữ, và phiên của quản
 * trị viên là thứ bị lấy. Danh sách trắng ở đây là TƯỜNG MINH và không có SVG.
 *
 * <h2>Vì sao phải giải mã lại rồi ghi lại</h2>
 *
 * <p>Một tệp có thể vừa là ảnh JPEG hợp lệ vừa là một thứ khác — tệp polyglot.
 * Kiểm "có đọc được thành ảnh không" thôi thì tệp đó đi lọt nguyên vẹn, kèm
 * toàn bộ phần đuôi mà bộ giải mã ảnh bỏ qua. Giải mã thành pixel rồi ghi ra
 * một tệp MỚI vứt đi mọi thứ không phải pixel: phần đuôi lạ, EXIF (gồm cả toạ
 * độ GPS của khách), và mọi khối metadata.
 *
 * <h2>Vì sao không tin Content-Type của client</h2>
 *
 * <p>Trường đó do trình duyệt — hoặc do một script — tự khai. Định dạng ở đây
 * được xác định bằng bộ đọc ImageIO nào nhận được nội dung thật, không phải
 * bằng lời client nói.
 */
@Component
public class ImageValidator {

    /** Định dạng được chấp nhận, theo TÊN BỘ ĐỌC của ImageIO (đã hạ chữ thường). */
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "jpg", "png", "webp");

    /**
     * Trần kích thước ảnh sau giải mã.
     *
     * <p>Một tệp PNG vài chục KB có thể khai kích thước 30000×30000; giải nén ra
     * là hàng chục GB trong bộ nhớ. Đây là trần chặn đúng thứ đó, và nó độc lập
     * với trần dung lượng tệp.
     */
    private static final int MAX_DIMENSION = 10_000;

    private final long maxBytes;

    public ImageValidator(@Value("${storage.max-image-bytes:5242880}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    /** Ảnh đã được chuẩn hoá: nội dung là do CHÍNH hệ thống này ghi ra. */
    public record ValidatedImage(byte[] content, String contentType, String extension) {}

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAdminRequest("Chưa chọn tệp ảnh.");
        }
        if (file.getSize() > maxBytes) {
            throw new ImageTooLarge("Ảnh vượt quá " + (maxBytes / 1024 / 1024) + "MB.");
        }

        byte[] raw = readAll(file);
        String format = detectFormat(raw);
        if (format == null || !ALLOWED_FORMATS.contains(format)) {
            throw new UnsupportedImageType(
                    "Chỉ nhận ảnh JPEG, PNG hoặc WebP. Tệp SVG không được chấp nhận.");
        }

        BufferedImage decoded = decode(raw);
        if (decoded.getWidth() > MAX_DIMENSION || decoded.getHeight() > MAX_DIMENSION) {
            throw new ImageTooLarge(
                    "Kích thước ảnh vượt quá " + MAX_DIMENSION + "×" + MAX_DIMENSION + " điểm ảnh.");
        }

        // WebP: thư viện đang dùng chỉ ĐỌC được, không ghi được. Ảnh WebP hợp lệ
        // vì thế được ghi lại thành PNG — vẫn đúng tinh thần "ghi lại bằng chính
        // bộ mã hoá của mình", chỉ đổi định dạng đầu ra.
        boolean jpeg = "jpeg".equals(format) || "jpg".equals(format);
        String outputFormat = jpeg ? "jpg" : "png";
        String contentType = jpeg ? "image/jpeg" : "image/png";
        byte[] normalized = encode(jpeg ? flattenAlpha(decoded) : decoded, outputFormat);

        return new ValidatedImage(normalized, contentType, outputFormat);
    }

    private static byte[] readAll(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new InvalidAdminRequest("Không đọc được tệp tải lên.");
        }
    }

    /** Định dạng thật, suy ra từ bộ đọc ImageIO nào nhận được nội dung. */
    private static String detectFormat(byte[] raw) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            return readers.hasNext()
                    ? readers.next().getFormatName().toLowerCase(Locale.ROOT)
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage decode(byte[] raw) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(raw));
            if (image == null) {
                throw new UnsupportedImageType("Nội dung tệp không phải ảnh đọc được.");
            }
            return image;
        } catch (UnsupportedImageType e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedImageType("Nội dung tệp không phải ảnh đọc được.");
        }
    }

    /**
     * JPEG không có kênh trong suốt.
     *
     * <p>Ghi thẳng một ảnh có alpha ra JPEG cho ra tệp hỏng hoặc ảnh ám hồng,
     * tuỳ phiên bản JDK. Trải phẳng lên nền trắng trước là cách duy nhất kết
     * quả ổn định.
     */
    private static BufferedImage flattenAlpha(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage flattened = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = flattened.createGraphics();
        graphics.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        graphics.dispose();
        return flattened;
    }

    private static byte[] encode(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, format, output)) {
                throw new UnsupportedImageType("Không ghi lại được ảnh ở định dạng " + format + ".");
            }
            return output.toByteArray();
        } catch (UnsupportedImageType e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedImageType("Không ghi lại được ảnh tải lên.");
        }
    }
}
