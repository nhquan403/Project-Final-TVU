package com.tvh.homestay.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đồng hồ của ứng dụng, cố định ở múi giờ Việt Nam.
 *
 * <p>Múi giờ được đặt ở ba tầng độc lập, không phải một: biến {@code TZ} cho
 * container, {@code -Duser.timezone} cho JVM, và bean này cho mã nghiệp vụ.
 * Lý do có tầng thứ ba: mọi phép tính ngày phải đúng kể cả khi ai đó quên biến
 * môi trường. Tiêm {@link Clock} này rồi gọi {@code LocalDate.now(clock)} thay
 * vì {@code LocalDate.now()} — hàm không tham số đọc múi giờ mặc định của JVM
 * và sẽ sai lệch một ngày trong khung 00:00–07:00 giờ Việt Nam khi JVM chạy UTC.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock appClock() {
        return Clock.system(APP_ZONE);
    }
}
