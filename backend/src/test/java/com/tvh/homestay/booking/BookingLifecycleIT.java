package com.tvh.homestay.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import com.tvh.homestay.booking.dto.BookingDtos.CreateBookingRequest;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.exception.BookingExceptions.BookingNotFound;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidAccessToken;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidStateTransition;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Vòng đời đơn, và việc ánh xạ ngoại lệ nghiệp vụ sang HTTP.
 *
 * <p>Đi qua cổng thật ở đây là cần thiết: phần đáng kiểm là mã trạng thái và
 * hình dạng {@code problem+json} mà client nhận được. Mỗi test dùng một IP giả
 * lập riêng để không đụng hạn mức tần suất của Phase 4 — hạn mức đó là thật và
 * đúng, không nới cho test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingLifecycleIT extends AbstractPostgresIT {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 4);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private BookingService bookings;

    @Autowired
    private BookingStateMachine stateMachine;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private String clientIp;

    @BeforeEach
    void cleanSlate() {
        BookingTestFixtures.reset(jdbc, 1);
        clientIp = "198.51.100." + (COUNTER++ % 200 + 1);
    }

    private static int COUNTER = 1;

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.add("X-Forwarded-For", clientIp);
        return h;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private ResponseEntity<Map> post(String path, Object body) {
        return rest.exchange(
                url(path), HttpMethod.POST, new HttpEntity<>(body, headers()), Map.class);
    }

    private CreateBookingRequest request(int seq) {
        return BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, seq);
    }

    @Test
    @DisplayName("POST /api/bookings → 201 kèm mã và accessToken; đặt lại khi hết phòng → 409 ROOM_NOT_AVAILABLE")
    void createThenSoldOutReturns409NotServerError() {
        ResponseEntity<Map> first = post("/api/bookings", request(1));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("code")).asString().startsWith("TVH");
        assertThat(first.getBody().get("accessToken")).asString().hasSize(32);
        // Thông tin thanh toán nằm trong object `payment` chứ không rải phẳng ở
        // gốc: màn hình QR cần cả nội dung chuyển khoản lẫn ảnh QR cùng lúc.
        @SuppressWarnings("unchecked")
        Map<String, Object> payment = (Map<String, Object>) first.getBody().get("payment");
        assertThat(payment)
                .as("đơn vừa tạo phải kèm sẵn thông tin để trả tiền")
                .isNotNull();
        assertThat(payment.get("transferContent"))
                .as("nội dung chuyển khoản = mã đơn + số thứ tự lần thanh toán")
                .isEqualTo(first.getBody().get("code") + "01");

        ResponseEntity<Map> second = post("/api/bookings", request(2));
        assertThat(second.getStatusCode())
                .as("hết phòng là tình huống nghiệp vụ, không phải lỗi máy chủ")
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().get("code")).isEqualTo("ROOM_NOT_AVAILABLE");
    }

    @Test
    @DisplayName("payment-status: có token → 200, thiếu token → 401")
    void paymentStatusRequiresAccessToken() {
        ResponseEntity<Map> created = post("/api/bookings", request(1));
        String code = (String) created.getBody().get("code");
        String token = (String) created.getBody().get("accessToken");

        ResponseEntity<Map> withToken = rest.exchange(
                url("/api/bookings/" + code + "/payment-status?token=" + token),
                HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
        assertThat(withToken.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Mã đơn nằm trên sao kê ngân hàng và dashboard nhà cung cấp, nên chỉ
        // biết mã thì không được phép theo dõi trạng thái đơn của người khác.
        ResponseEntity<Map> withoutToken = rest.exchange(
                url("/api/bookings/" + code + "/payment-status"),
                HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
        assertThat(withoutToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(withoutToken.getBody().get("code")).isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    @DisplayName("Tra cứu sai số điện thoại → 404, không rò rỉ gì về đơn")
    void lookupWithWrongPhoneRevealsNothing() {
        ResponseEntity<Map> created = post("/api/bookings", request(1));
        String code = (String) created.getBody().get("code");

        ResponseEntity<Map> wrong =
                post("/api/bookings/lookup", Map.of("code", code, "phone", "0999999999"));
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(wrong.getBody().get("code")).isEqualTo("BOOKING_NOT_FOUND");
        assertThat(wrong.getBody()).doesNotContainKey("guestName");

        // Mã không tồn tại trả ĐÚNG cùng một phản hồi: phân biệt hai trường hợp
        // là cho người dò biết mã nào có thật rồi dò tiếp số điện thoại.
        ResponseEntity<Map> missing =
                post("/api/bookings/lookup", Map.of("code", "TVH000000", "phone", "0900000001"));
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody().get("code")).isEqualTo("BOOKING_NOT_FOUND");
    }

    @Test
    @DisplayName("Huỷ đơn: phòng mở lại ngay, lịch sử vẫn còn")
    void cancellingReleasesRoomImmediately() {
        BookingResponse booking =
                bookings.create(request(1), null, "127.0.0.1", "junit");

        stateMachine.transition(
                bookingRepository.findByCode(booking.code()).orElseThrow(),
                BookingStatus.CANCELLED, HistoryActor.GUEST, "Đổi kế hoạch");

        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM booking_rooms WHERE status = 'ACTIVE'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM booking_rooms WHERE status = 'RELEASED'", Integer.class))
                .as("lịch sử gán phòng vẫn tra cứu được")
                .isEqualTo(1);

        // Phòng thật sự dùng lại được, không chỉ đổi chữ trong cột trạng thái.
        assertThatCode(() -> bookings.create(request(2), null, "127.0.0.1", "junit"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Chuyển trạng thái sai bị từ chối")
    void invalidTransitionIsRejected() {
        BookingResponse booking = bookings.create(request(1), null, "127.0.0.1", "junit");
        var entity = bookingRepository.findByCode(booking.code()).orElseThrow();

        assertThatThrownBy(() -> stateMachine.transition(
                        entity, BookingStatus.CHECKED_OUT, HistoryActor.ADMIN, null))
                .isInstanceOf(InvalidStateTransition.class);
    }

    @Test
    @DisplayName("Mã truy cập sai → 401; mã đơn không tồn tại → 404")
    void tokenAndCodeErrorsAreDistinct() {
        BookingResponse booking = bookings.create(request(1), null, "127.0.0.1", "junit");

        assertThatThrownBy(() -> bookings.requireByCodeAndToken(booking.code(), "sai-token"))
                .isInstanceOf(InvalidAccessToken.class);
        assertThatThrownBy(() -> bookings.requireByCodeAndToken("TVH000000", "bat-ky"))
                .isInstanceOf(BookingNotFound.class);
    }

    @Test
    @DisplayName("Mã khuyến mãi usage_limit NULL dùng được không giới hạn")
    void unlimitedPromotionIsNeverExhausted() {
        BookingTestFixtures.reset(jdbc, 3);
        jdbc.update("""
                INSERT INTO promotions (id, code, name, discount_type, discount_value,
                                        starts_at, ends_at, usage_limit, used_count)
                VALUES (1, 'VOHAN', 'Không giới hạn', 'PERCENT', 10,
                        now() - interval '1 day', now() + interval '30 day', NULL, 0)
                """);

        // Thiếu mệnh đề `usage_limit IS NULL OR` trong câu tiêu thụ thì so sánh
        // cho ra NULL, không dòng nào được cập nhật, và mã vô hạn bị từ chối
        // ngay ở lần dùng đầu tiên.
        for (int seq = 1; seq <= 3; seq++) {
            final int current = seq;
            assertThatCode(() -> bookings.create(
                            new CreateBookingRequest(
                                    BookingTestFixtures.ROOM_TYPE_ID, CHECK_IN, CHECK_OUT, 1, 2, 0,
                                    "Khách " + current, "k" + current + "@example.com",
                                    "090000001" + current, null, "VOHAN"),
                            null, "127.0.0.1", "junit"))
                    .doesNotThrowAnyException();
        }
        assertThat(jdbc.queryForObject("SELECT used_count FROM promotions WHERE id = 1", Integer.class))
                .isEqualTo(3);
    }
}
