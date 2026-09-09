package com.tvh.homestay.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.schema.AbstractPostgresIT;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Lưới chắn chống endpoint bị bỏ quên khỏi ma trận phân quyền.
 *
 * <p>Quét mọi endpoint ĐÃ ĐĂNG KÝ THẬT trong {@code RequestMappingHandlerMapping}
 * và fail nếu có cái nào không nằm trong ma trận. Nhờ vậy, một phase sau thêm
 * controller mới mà quên cập nhật {@code SecurityConfig} sẽ làm test đỏ ngay,
 * thay vì để endpoint đó rơi vào {@code denyAll()} rồi mãi sau mới phát hiện
 * qua một lỗi 401 khó hiểu trên giao diện.
 *
 * <p><b>Kiểm một chiều, có chủ ý.</b> Chỉ soi "endpoint có thật mà thiếu trong
 * ma trận", KHÔNG soi chiều ngược lại. Ma trận cố ý khai trước nhiều đường dẫn
 * của Phase 5 đến Phase 8; bắt lỗi chiều ngược lại sẽ khiến test đỏ liên tục từ
 * đây cho tới khi phase cuối cùng hoàn thành, và một test đỏ triền miên là test
 * bị bỏ qua.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class EndpointAuthorizationIT extends AbstractPostgresIT {

    /**
     * Ma trận phân quyền, dạng tiền tố đường dẫn. Phải khớp với
     * {@code SecurityConfig}; đây là bản dùng để đối chiếu, không phải bản thi
     * hành.
     */
    private static final List<String> AUTHORIZATION_MATRIX = List.of(
            "/api/health",
            "/api/room-types",
            "/api/availability",
            "/api/content",
            "/api/posts",
            "/api/reviews",
            "/api/promotions/check",
            "/api/bookings",
            "/api/payments/webhook/sepay",
            "/uploads",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/change-password",
            "/api/me",
            "/api/admin",
            "/swagger-ui",
            "/v3/api-docs");

    /** Đường dẫn hạ tầng do Spring Boot tự đăng ký, không thuộc bề mặt API. */
    private static final List<String> INFRASTRUCTURE_PREFIXES = List.of("/error", "/actuator");

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("Mọi endpoint đã đăng ký đều nằm trong ma trận phân quyền")
    void everyRegisteredEndpointAppearsInTheMatrix() {
        Set<String> uncovered = new TreeSet<>();

        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            for (String pattern : patternsOf(info)) {
                if (isInfrastructure(pattern) || isCovered(pattern)) {
                    continue;
                }
                uncovered.add(pattern);
            }
        }

        assertThat(uncovered)
                .as("Endpoint có thật nhưng không có dòng nào trong ma trận phân quyền. "
                        + "Thêm dòng vào SecurityConfig VÀ vào AUTHORIZATION_MATRIX của test này.")
                .isEmpty();
    }

    @Test
    @DisplayName("Sáu endpoint xác thực của phase này đã đăng ký thật")
    void authEndpointsAreRegistered() {
        Set<String> registered = new TreeSet<>();
        handlerMapping.getHandlerMethods().keySet()
                .forEach(info -> registered.addAll(patternsOf(info)));

        assertThat(registered).contains(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/api/auth/change-password",
                "/api/me");
    }

    private static Set<String> patternsOf(RequestMappingInfo info) {
        Set<String> patterns = new TreeSet<>();
        if (info.getPathPatternsCondition() != null) {
            info.getPathPatternsCondition().getPatterns()
                    .forEach(pattern -> patterns.add(pattern.getPatternString()));
        }
        return patterns;
    }

    private static boolean isInfrastructure(String pattern) {
        return INFRASTRUCTURE_PREFIXES.stream().anyMatch(pattern::startsWith);
    }

    private static boolean isCovered(String pattern) {
        return AUTHORIZATION_MATRIX.stream().anyMatch(pattern::startsWith);
    }
}
