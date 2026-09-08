package com.tvh.homestay.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Dừng khởi động khi thiếu bí mật bắt buộc.
 *
 * <p>Repo này công khai. Một giá trị mặc định "an toàn cho demo" nằm trong mã
 * nguồn đồng nghĩa với việc bất kỳ ai clone về cũng tự ký được JWT vai trò
 * ADMIN cho mọi bản triển khai dùng repo này, và giả mạo được webhook xác nhận
 * thanh toán. Nên không biến nào ở đây có giá trị mặc định — ở bất kỳ profile
 * nào, kể cả {@code demo}.
 *
 * <p>Chạy trong {@code @PostConstruct} chứ không phải {@code ApplicationRunner}
 * để hỏng ngay lúc dựng context, trước khi web server kịp lắng nghe.
 */
@Component
public class RequiredSecretsValidator {

    private static final Logger log = LoggerFactory.getLogger(RequiredSecretsValidator.class);

    /** Độ dài tối thiểu của khoá ký HS256, tính theo byte. */
    private static final int MIN_JWT_SECRET_BYTES = 32;

    private final Environment environment;

    public RequiredSecretsValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        List<String> problems = new ArrayList<>();

        requirePresent("JWT_SECRET", problems);
        requirePresent("SEPAY_WEBHOOK_API_KEY", problems);

        String jwtSecret = environment.getProperty("JWT_SECRET");
        if (jwtSecret != null && jwtSecret.getBytes().length < MIN_JWT_SECRET_BYTES) {
            problems.add("JWT_SECRET quá ngắn: cần tối thiểu " + MIN_JWT_SECRET_BYTES
                    + " byte. Sinh bằng: openssl rand -base64 48");
        }

        if (!problems.isEmpty()) {
            // Chỉ log tên biến và mô tả vấn đề. Không bao giờ log giá trị.
            problems.forEach(problem -> log.error("Cấu hình thiếu hoặc không hợp lệ: {}", problem));
            throw new IllegalStateException(
                    "Thiếu cấu hình bắt buộc: " + String.join("; ", problems)
                            + ". Xem .env.example để biết cách sinh giá trị.");
        }
    }

    private void requirePresent(String variableName, List<String> problems) {
        String value = environment.getProperty(variableName);
        if (value == null || value.isBlank()) {
            problems.add(variableName + " chưa được đặt");
        }
    }
}
