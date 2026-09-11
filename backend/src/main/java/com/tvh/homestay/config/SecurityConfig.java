package com.tvh.homestay.config;

import com.tvh.homestay.auth.JwtAuthenticationFilter;
import com.tvh.homestay.auth.MustChangePasswordFilter;
import com.tvh.homestay.common.RateLimitFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Ma trận phân quyền của toàn hệ thống.
 *
 * <p><b>Chuỗi kết thúc bằng {@code denyAll()}.</b> Endpoint chưa được liệt kê ở
 * đây là ĐÓNG, không phải mở. Mặc định mở là cách nhanh nhất để một endpoint
 * mới lặng lẽ lọt ra ngoài mà không ai nhận ra — và {@code EndpointAuthorizationIT}
 * là lưới chắn thứ hai: nó fail khi có endpoint thật không nằm trong ma trận.
 *
 * <p>Nhiều dòng dưới đây trỏ tới endpoint của các phase sau. Chúng được khai
 * trước là có chủ ý: quên khai một dòng khi phase đó tới sẽ khiến endpoint bị
 * 401 một cách khó hiểu. Ví dụ quên {@code payment-status} nghĩa là màn hình QR
 * ở Phase 6 không bao giờ chuyển sang trang cảm ơn.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final MustChangePasswordFilter mustChangePasswordFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            MustChangePasswordFilter mustChangePasswordFilter,
            RateLimitFilter rateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.mustChangePasswordFilter = mustChangePasswordFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF tắt được vì API dùng Bearer token, không dùng cookie phiên.
                // Endpoint refresh có dùng cookie, nhưng cookie đó là
                // SameSite=Strict nên trình duyệt không gửi kèm request khởi
                // phát từ site khác — điều kiện tiên quyết của CSRF không còn.
                .csrf(csrf -> csrf.disable())
                // Spring Security tự tìm bean TÊN LÀ corsConfigurationSource.
                // Không tiêm theo KIỂU: Spring MVC cũng đăng ký một bean cùng
                // kiểu (mvcHandlerMappingIntrospector) nên tiêm theo kiểu sẽ
                // hỏng với lỗi "expected single matching bean but found 2".
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Trang lỗi không phải một endpoint cần xin phép riêng.
                        // Servlet container gửi lại request dưới dạng dispatch
                        // ERROR, và SecurityContext KHÔNG đi theo lượt dispatch
                        // đó — nên nếu không mở ở đây, mọi lỗi 404 hay 500 của
                        // người đã đăng nhập đều hiện ra thành 401 "hãy đăng
                        // nhập", che mất lỗi thật.
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()

                        // ─── Công khai ───────────────────────────────────────
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/room-types/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/availability", "/api/availability/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/content/**", "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/promotions/check").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // ─── Đặt phòng: công khai, tự kiểm access_token/phone ở
                        //     tầng service. Quyền ở đây là "vào được", còn
                        //     "đúng chủ đơn hay không" là việc của Phase 5. ──
                        .requestMatchers(HttpMethod.POST, "/api/bookings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/bookings/lookup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/cancel").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bookings/*/payment-status").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/review").permitAll()

                        // Webhook tự xác thực bằng API key của nhà cung cấp.
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook/sepay").permitAll()

                        // ─── Xác thực ────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register", "/api/auth/login", "/api/auth/refresh")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/logout", "/api/auth/change-password")
                        .authenticated()

                        // ─── Tài liệu API: chỉ ở profile demo ────────────────
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ─── Có tài khoản ────────────────────────────────────
                        .requestMatchers("/api/me", "/api/me/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Endpoint mô phỏng thanh toán cần ADMIN VÀ cờ
                        // payments.simulator.enabled (Phase 6 kiểm cờ đó).
                        .requestMatchers("/api/admin/dev/**").hasRole("ADMIN")
                        // MỘT dòng cho toàn bộ khu quản trị, có chủ ý: liệt kê
                        // ba mươi đường dẫn ở đây là tạo ra ba mươi chỗ để lệch
                        // nhau, và chỉ cần một chỗ khai thiếu là một endpoint
                        // quản trị rơi ra ngoài. Việc bắt người viết PHẢI khai
                        // từng nhóm endpoint mới thuộc về EndpointAuthorizationIT,
                        // nơi khai sai chỉ làm test đỏ chứ không mở cửa.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ─── Mặc định đóng ───────────────────────────────────
                        .anyRequest().denyAll())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(mustChangePasswordFilter, JwtAuthenticationFilter.class)
                // Mặc định của Spring Security khi không có cơ chế đăng nhập
                // nào là trả 403 cho cả request KHÔNG có token. Phân biệt hai
                // trường hợp là bắt buộc: 401 nghĩa là "hãy đăng nhập", 403
                // nghĩa là "đã đăng nhập nhưng không đủ quyền". Client xử lý
                // hai tình huống đó hoàn toàn khác nhau.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                writeProblem(response, 401, "UNAUTHORIZED",
                                        "Cần đăng nhập để dùng chức năng này."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeProblem(response, 403, "FORBIDDEN",
                                        "Tài khoản không có quyền dùng chức năng này.")))
                .build();
    }

    /** Lỗi bảo mật trả dạng problem+json, không stack trace, không lộ nội bộ. */
    private static void writeProblem(
            jakarta.servlet.http.HttpServletResponse response, int status, String title, String detail)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                        + ",\"detail\":\"" + detail + "\"}");
    }

    /**
     * BCrypt cost 10 (mặc định của Spring Security).
     *
     * <p>Cost là số mũ: mỗi bậc tăng gấp đôi thời gian băm. 10 là điểm cân bằng
     * hiện hành giữa chi phí cho kẻ dò và độ trễ của lần đăng nhập hợp lệ.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * CORS liệt kê tường minh origin, đọc từ biến môi trường.
     *
     * <p>Không bao giờ dùng {@code *}: khi bật {@code allowCredentials}, cookie
     * refresh token sẽ được gửi kèm request từ BẤT KỲ site nào, và trình duyệt
     * cũng từ chối tổ hợp đó nên lỗi lại xuất hiện dưới dạng "lỗi CORS" khó lần.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
