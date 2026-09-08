package com.tvh.homestay.common;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.TimeZone;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint kiểm tra sức khoẻ, đồng thời là bằng chứng múi giờ được đặt đúng.
 *
 * <p>Trả về hai trường thời gian có chủ đích. {@code time} lấy từ đồng hồ ứng
 * dụng nên luôn mang offset +07:00. {@code jvmDefaultZone} phơi bày múi giờ mặc
 * định thật của JVM — nếu nó ra {@code UTC} thì biến môi trường đã bị thiếu, và
 * mọi phép gom nhóm theo tháng ở dashboard sẽ lệch cho các booking tạo trong
 * khung 00:00–07:00. Chỉ nhìn {@code time} thì không phát hiện được điều đó.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final Clock clock;

    public HealthController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "time", OffsetDateTime.now(clock).toString(),
                "appZone", clock.getZone().getId(),
                "jvmDefaultZone", TimeZone.getDefault().getID());
    }
}
