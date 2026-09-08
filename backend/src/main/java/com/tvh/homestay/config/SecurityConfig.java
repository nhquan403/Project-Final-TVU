package com.tvh.homestay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật tạm của Phase 1: mở đúng một endpoint, đóng phần còn lại.
 *
 * <p>Phase 4 thay bằng ma trận phân quyền đầy đủ. Nguyên tắc giữ nguyên qua mọi
 * phase: chuỗi filter kết thúc bằng {@code denyAll()}, nên endpoint nào chưa
 * được liệt kê là đóng chứ không phải mở. Mặc định mở là cách nhanh nhất để một
 * endpoint mới lặng lẽ lọt ra ngoài mà không ai nhận ra.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .anyRequest().denyAll())
                .build();
    }
}
