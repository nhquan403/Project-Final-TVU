package com.tvh.homestay.config;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import java.time.Clock;

/**
 * Bật tự điền {@code created_at} / {@code updated_at}.
 *
 * <p>Dùng bean {@link Clock} của ứng dụng thay vì {@code OffsetDateTime.now()}
 * mặc định: cùng một nguồn thời gian với phần còn lại của hệ thống, và test
 * thay được đồng hồ mà không phải đợi thời gian thật trôi.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock));
    }
}
