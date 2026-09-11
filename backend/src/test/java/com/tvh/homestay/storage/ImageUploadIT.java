package com.tvh.homestay.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.admin.AdminTestSupport;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Tải ảnh lên: danh sách trắng, giải mã lại, và ba header khi phục vụ ảnh.
 *
 * <p>Test chạy KHÔNG có {@code CLOUDINARY_URL}, nên đường đang kiểm chính là
 * {@link LocalImageStorage} — đúng đường sẽ chạy ở bản đem đi bảo vệ khi không
 * có tài khoản Cloudinary hoặc không có mạng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "booking.expiry-scan-ms=3600000")
class ImageUploadIT extends AbstractPostgresIT {

    /**
     * Mỗi lớp test đóng vai một máy khách riêng, và tài khoản quản trị được
     * dựng ĐÚNG MỘT LẦN cho cả lớp.
     *
     * <p>Hạn mức {@code /api/auth/**} là 10 request/phút cho một IP. Dựng lại
     * tài khoản ở từng test (đăng ký + hai lần đăng nhập) sẽ vượt hạn mức ngay
     * ở test thứ tư. Hạn mức đó là thật và đúng — không nới nó cho test.
     */
    private static final String CLIENT_IP = "198.51.100.77";

    private static final String CUSTOMER_IP = "198.51.100.78";

    private static String cachedAdminToken;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ImageStorageService storage;

    private String adminToken() {
        if (cachedAdminToken == null) {
            cachedAdminToken =
                    AdminTestSupport.registerAdmin(rest, port, jdbc, CLIENT_IP).accessToken();
        }
        return cachedAdminToken;
    }

    @Test
    @DisplayName("Bản chạy không có CLOUDINARY_URL dùng LocalImageStorage")
    void fallsBackToLocalStorageWithoutCloudinary() {
        assertThat(storage).isInstanceOf(LocalImageStorage.class);
    }

    @Test
    @DisplayName("Ảnh JPEG hợp lệ 3MB được nhận — giới hạn 5MB phải thật sự chạm tới được")
    void validThreeMegabyteImageIsAccepted() {
        byte[] png = noisyPng(1000, 1000);
        assertThat(png.length)
                .as("ảnh mẫu phải vượt xa mức mặc định 1MB của Spring Boot")
                .isGreaterThan(2_000_000);

        ResponseEntity<Map> response = upload(adminToken(), "anh-phong.png", "image/png", png);

        assertThat(response.getStatusCode())
                .as("mặc định spring.servlet.multipart.max-file-size là 1MB; không nâng lên thì "
                        + "ảnh này bị chặn ở tầng servlet trước khi ImageValidator kịp chạy")
                .isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(response.getBody().get("url"))).startsWith("/uploads/");
    }

    @Test
    @DisplayName("Ảnh tải lên được phục vụ lại kèm nosniff và Content-Type do máy chủ quyết định")
    void servedImageCarriesSecurityHeaders() {
        ResponseEntity<Map> uploaded =
                upload(adminToken(), "anh.png", "image/png", noisyPng(60, 60));
        String url = String.valueOf(uploaded.getBody().get("url"));

        ResponseEntity<byte[]> served = rest.exchange(
                "http://localhost:" + port + url, HttpMethod.GET, null, byte[].class);

        assertThat(served.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(served.getHeaders().getFirst("X-Content-Type-Options"))
                .as("thiếu nosniff thì trình duyệt tự đoán kiểu nội dung, và thứ nó đoán ra có thể "
                        + "chạy như HTML cùng origin với ứng dụng")
                .isEqualTo("nosniff");
        assertThat(served.getHeaders().getFirst("Content-Type")).isEqualTo("image/png");
    }

    @Test
    @DisplayName("SVG chứa script bị từ chối 415, dù trình duyệt gọi nó là ảnh")
    void svgWithScriptIsRejected() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                  <script>fetch('/api/admin/bookings').then(r=>r.text())</script>
                </svg>
                """;

        ResponseEntity<Map> response = upload(
                adminToken(), "xss.svg", "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().get("code")).isEqualTo("UNSUPPORTED_IMAGE_TYPE");
    }

    @Test
    @DisplayName("Tệp thực thi đội lốt .jpg bị từ chối — tên và Content-Type do client khai không được tin")
    void executableDisguisedAsJpegIsRejected() {
        // MZ là magic bytes của tệp thực thi Windows. Client khai là image/jpeg
        // và đặt tên .jpg; cả hai đều do client tự nói.
        byte[] fake = new byte[2048];
        fake[0] = 'M';
        fake[1] = 'Z';

        ResponseEntity<Map> response = upload(adminToken(), "anh.jpg", "image/jpeg", fake);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("Ảnh JPEG được GHI LẠI: phần đuôi lạ gắn sau tệp không đi lọt")
    void appendedPayloadIsStrippedByReEncoding() throws Exception {
        byte[] jpeg = jpeg(80, 80);
        byte[] polyglot = new byte[jpeg.length + 64];
        System.arraycopy(jpeg, 0, polyglot, 0, jpeg.length);
        System.arraycopy("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8), 0,
                polyglot, jpeg.length, 25);

        ResponseEntity<Map> response = upload(adminToken(), "anh.jpg", "image/jpeg", polyglot);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String url = String.valueOf(response.getBody().get("url"));
        byte[] stored = rest.getForObject("http://localhost:" + port + url, byte[].class);
        assertThat(new String(stored, StandardCharsets.ISO_8859_1))
                .as("nội dung được ghi lại từ pixel, nên phần đuôi gắn thêm biến mất")
                .doesNotContain("<script>");
    }

    @Test
    @DisplayName("Không token → 401; token CUSTOMER → 403")
    void uploadRequiresAdminRole() {
        byte[] png = noisyPng(40, 40);

        ResponseEntity<Map> anonymous = rest.exchange(
                "http://localhost:" + port + "/api/admin/images",
                HttpMethod.POST,
                new HttpEntity<>(multipart("anh.png", "image/png", png), multipartHeaders(null)),
                Map.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String customerToken =
                AdminTestSupport.registerCustomer(rest, port, jdbc, CUSTOMER_IP).accessToken();
        ResponseEntity<Map> customer = upload(customerToken, "anh.png", "image/png", png);
        assertThat(customer.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─────────────────────────────────────────────────────────────────────

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> upload(
            String token, String filename, String contentType, byte[] content) {
        return rest.exchange(
                "http://localhost:" + port + "/api/admin/images",
                HttpMethod.POST,
                new HttpEntity<>(multipart(filename, contentType, content), multipartHeaders(token)),
                Map.class);
    }

    private static HttpHeaders multipartHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("X-Forwarded-For", CLIENT_IP);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private static MultiValueMap<String, Object> multipart(
            String filename, String contentType, byte[] content) {
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, partHeaders));
        body.add("folder", "room-types");
        return body;
    }

    /**
     * PNG nhiễu ngẫu nhiên.
     *
     * <p>Nhiễu chứ không phải một màu: PNG nén mất dữ liệu bằng không, nên một
     * ảnh một màu 1000×1000 chỉ nặng vài KB và sẽ không chạm tới giới hạn dung
     * lượng mà test này sinh ra để kiểm.
     */
    private static byte[] noisyPng(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        return write(image, "png");
    }

    private static byte[] jpeg(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, (x * 7 + y * 11) % 0xFFFFFF);
            }
        }
        return write(image, "jpg");
    }

    private static byte[] write(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, format, output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
