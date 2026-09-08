package com.tvh.homestay.schema;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Nền chung cho test schema: một PostgreSQL 16 THẬT qua Testcontainers.
 *
 * <p><b>Vì sao không dùng H2.</b> H2 không có kiểu {@code daterange} lẫn ràng
 * buộc {@code EXCLUDE}. Test chạy trên H2 sẽ xanh mà không chứng minh được
 * điều duy nhất đáng chứng minh ở phase này — rằng cơ sở dữ liệu tự nó chặn
 * đặt trùng phòng.
 *
 * <p>Container dùng chung cho mọi lớp test kế thừa (khởi động một lần, không
 * gọi {@code stop()}: Ryuk dọn khi JVM thoát). Dựng lại Postgres cho từng lớp
 * test tốn nhiều thời gian hơn phần việc thật.
 *
 * <p>Hai bí mật bắt buộc được sinh NGẪU NHIÊN mỗi lần chạy chứ không ghi sẵn
 * vào file cấu hình test. Repo này công khai; một giá trị "chỉ để test" nằm
 * trong repo vẫn là một giá trị nằm trong repo, và nó sẽ bị copy sang chỗ
 * khác. Cách này cũng chứng minh luôn rằng ứng dụng thật sự đòi hai biến đó.
 */
public abstract class AbstractPostgresIT {

    /**
     * Ghim phiên bản Docker API mà docker-java gửi đi.
     *
     * <p>Không có dòng này, docker-java thương lượng bằng API 1.32 và Docker
     * Engine từ bản 29 trả về {@code 400 client version 1.32 is too old.
     * Minimum supported API version is 1.40} — Testcontainers báo "Could not
     * find a valid Docker environment", một thông báo dẫn người đọc đi sai
     * hướng hoàn toàn (tưởng máy chưa cài Docker).
     *
     * <p>Chọn đúng 1.40 chứ không phải bản mới nhất: đó vừa là mức tối thiểu
     * Docker 29 chấp nhận, vừa là mức Docker 19.03 (2019) trở đi đều hỗ trợ.
     * Test vì thế chạy được trên cả máy có Docker cũ lẫn máy có Docker mới,
     * không phải đặt biến môi trường nào.
     */
    static {
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.40");
        }
    }

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("homestay")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withEnv("TZ", "Asia/Ho_Chi_Minh")
                    .withEnv("PGTZ", "Asia/Ho_Chi_Minh");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USER", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("JWT_SECRET", () -> randomSecret(48));
        registry.add("SEPAY_WEBHOOK_API_KEY", () -> randomSecret(32));
    }

    private static String randomSecret(int bytes) {
        byte[] buffer = new byte[bytes];
        new SecureRandom().nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }
}
