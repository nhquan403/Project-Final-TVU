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
 * <h2>Vì sao khu quản trị không có một dòng bao</h2>
 *
 * <p>Bản đầu của ma trận này có đúng một dòng {@code "/api/admin"}. Vì
 * {@link #isCovered} so theo TIỀN TỐ, dòng đó bao trọn mọi endpoint quản trị —
 * thêm ba mươi endpoint mới cũng không làm test đỏ, và lưới chắn được tuyên bố
 * ở kế hoạch Phase 7 thực ra không bắt được gì.
 *
 * <p>Nay mỗi nhóm endpoint quản trị là một dòng riêng. Việc thi hành phân quyền
 * vẫn nằm ở MỘT dòng {@code /api/admin/**} trong {@code SecurityConfig} — một
 * quy tắc bảo mật duy nhất vẫn tốt hơn ba mươi quy tắc để lệch nhau. Cái được
 * siết ở đây là SỰ CÓ Ý THỨC: thêm một nhóm endpoint quản trị mới buộc người
 * viết phải khai nó ra ở đây, tức là phải dừng lại nghĩ một lần xem nhóm đó có
 * thật sự chỉ dành cho ADMIN hay không.
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
            // Khu quản trị được liệt kê THEO TỪNG NHÓM, không phải một dòng
            // "/api/admin" bao hết. Xem javadoc bên dưới.
            "/api/admin/bookings",
            "/api/admin/content",
            "/api/admin/payments",
            "/api/admin/room-types",
            "/api/admin/amenities",
            "/api/admin/rooms",
            "/api/admin/promotions",
            "/api/admin/reviews",
            "/api/admin/dashboard",
            "/api/admin/reports",
            "/api/admin/images",
            "/api/admin/dev",
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

    /**
     * Lưới chắn cho chính lưới chắn.
     *
     * <p>Một người sửa sau này thấy test đỏ vì quên khai endpoint mới có thể
     * "sửa" bằng cách thêm lại dòng bao {@code "/api/admin"}. Làm vậy là tắt
     * lưới chắn mà test vẫn xanh — đúng kiểu hỏng tệ nhất. Kiểm này khiến cách
     * sửa đó không đi lọt.
     */
    @Test
    @DisplayName("Ma trận KHÔNG được có dòng bao \"/api/admin\" — nó vô hiệu hoá chính kiểm tra trên")
    void adminPrefixMustBeListedPerGroup() {
        assertThat(AUTHORIZATION_MATRIX)
                .as("Khai theo từng nhóm (/api/admin/bookings, /api/admin/rooms, …), "
                        + "không dùng một dòng bao hết")
                .doesNotContain("/api/admin", "/api/admin/", "/api/admin/**");
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
