package com.tvh.homestay.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.booking.BookingService;
import com.tvh.homestay.booking.BookingStateMachine;
import com.tvh.homestay.booking.BookingTestFixtures;
import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.time.LocalDate;
import java.util.List;
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
 * Gửi đánh giá — và những cách nó KHÔNG được phép hoạt động.
 *
 * <p>Mỗi test dùng một IP giả lập riêng: hạn mức {@code ip:review} là 5 lượt mỗi
 * phút và nó là thật, không nới cho test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "booking.expiry-scan-ms=3600000")
class ReviewIT extends AbstractPostgresIT {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 10, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 10, 3);

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

    private static int counter;
    private String clientIp;

    @BeforeEach
    void cleanSlate() {
        jdbc.execute("DELETE FROM reviews");
        BookingTestFixtures.reset(jdbc, 2);
        clientIp = "203.0.113." + (++counter % 200 + 10);
    }

    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Không token và không SĐT → 404, không lộ đơn có thật hay không")
    void submittingWithoutIdentityIsRejected() {
        BookingResponse booking = checkedOutBooking(1);

        ResponseEntity<Map> response = submit(booking.code(), Map.of(
                "rating", 5, "title", "Tuyệt vời", "content", "Phòng sạch"));

        // Mã đơn chỉ chín ký tự và nằm trên sao kê ngân hàng. Không kiểm danh
        // tính thì ai thấy sao kê cũng đăng được đánh giá một sao ĐỨNG TÊN
        // khách thật — vì tên do hệ thống tự chụp từ đơn.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(countReviews()).isZero();
    }

    @Test
    @DisplayName("Sai số điện thoại → 404 giống hệt mã không tồn tại, không lộ gì")
    void wrongPhoneLooksExactlyLikeAnUnknownCode() {
        BookingResponse booking = checkedOutBooking(1);

        ResponseEntity<Map> wrongPhone = submit(booking.code(), Map.of(
                "phone", "0999999999", "rating", 5, "content", "x"));
        ResponseEntity<Map> unknownCode = submit("TVHKHONGCO", Map.of(
                "phone", "0900000001", "rating", 5, "content", "x"));

        assertThat(wrongPhone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownCode.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(wrongPhone.getBody().get("code"))
                .as("hai tình huống phải trả về cùng một mã lỗi, nếu không là dò được mã nào có thật")
                .isEqualTo(unknownCode.getBody().get("code"));
    }

    @Test
    @DisplayName("Đúng access token → gửi được, đánh giá vào hàng CHỜ DUYỆT")
    void validTokenCreatesPendingReview() {
        BookingResponse booking = checkedOutBooking(1);

        ResponseEntity<Map> response = submit(booking.code(), Map.of(
                "token", booking.accessToken(),
                "rating", 5,
                "title", "Rất đáng tiền",
                "content", "Chủ nhà thân thiện, phòng sạch."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(statusOfReview()).isEqualTo("PENDING");
        assertThat(response.getBody().get("guestName"))
                .as("tên lấy từ ĐƠN, không nhận từ thân request")
                .isEqualTo(booking.guestName());
    }

    @Test
    @DisplayName("Đúng số điện thoại cũng gửi được — khách vãng lai mất link vẫn đánh giá được")
    void validPhoneAlsoWorks() {
        BookingResponse booking = checkedOutBooking(1);

        ResponseEntity<Map> response = submit(booking.code(), Map.of(
                "phone", booking.guestPhone(), "rating", 4, "content", "Ổn"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Đơn chưa trả phòng → bị từ chối")
    void onlyCheckedOutBookingsCanBeReviewed() {
        BookingResponse booking =
                bookings.create(BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, 1),
                        null, "127.0.0.1", "junit");

        ResponseEntity<Map> response = submit(booking.code(), Map.of(
                "token", booking.accessToken(), "rating", 5, "content", "x"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(countReviews()).isZero();
    }

    @Test
    @DisplayName("Gửi lần hai cho cùng một đơn → bị chặn")
    void secondReviewForTheSameBookingIsBlocked() {
        BookingResponse booking = checkedOutBooking(1);
        Map<String, Object> body = Map.of(
                "token", booking.accessToken(), "rating", 5, "content", "Lần đầu");

        assertThat(submit(booking.code(), body).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<Map> second = submit(booking.code(), body);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(countReviews()).isEqualTo(1);
    }

    @Test
    @DisplayName("Nội dung chứa thẻ HTML được lưu NGUYÊN VĂN, không bị diễn giải và không bị escape")
    void hostileContentIsStoredVerbatim() {
        BookingResponse booking = checkedOutBooking(1);
        String hostile = "<img src=x onerror=alert(1)>";

        submit(booking.code(), Map.of(
                "token", booking.accessToken(),
                "rating", 1,
                "title", hostile,
                "content", "Tôi & gia đình <b>không</b> hài lòng"));

        String storedTitle = jdbc.queryForObject(
                "SELECT title FROM reviews", String.class);
        String storedContent = jdbc.queryForObject(
                "SELECT content FROM reviews", String.class);

        // Lưu nguyên văn là ĐÚNG: lớp phòng thủ nằm ở nơi hiển thị (text
        // binding của Angular), không ở nơi ghi. Escape lúc ghi sẽ biến dấu `&`
        // của khách thành `&amp;` trên màn hình.
        assertThat(storedTitle).isEqualTo(hostile);
        assertThat(storedContent).isEqualTo("Tôi & gia đình <b>không</b> hài lòng");
    }

    @Test
    @DisplayName("/api/reviews chỉ trả đánh giá ĐÃ DUYỆT")
    void publicListOnlyShowsApprovedReviews() {
        BookingResponse first = checkedOutBooking(1);
        BookingResponse second = checkedOutBooking(2);
        submit(first.code(), Map.of("token", first.accessToken(), "rating", 5, "content", "Chờ duyệt"));
        submit(second.code(), Map.of("token", second.accessToken(), "rating", 4, "content", "Đã duyệt"));
        jdbc.update("UPDATE reviews SET status = 'APPROVED' WHERE content = 'Đã duyệt'");

        ResponseEntity<List> response =
                rest.getForEntity("http://localhost:" + port + "/api/reviews", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().toString())
                .as("kiểm duyệt chỉ có nghĩa khi nửa còn lại — bộ lọc ở truy vấn — cũng tồn tại")
                .contains("Đã duyệt")
                .doesNotContain("Chờ duyệt");
    }

    // ─────────────────────────────────────────────────────────────────────

    private BookingResponse checkedOutBooking(int seq) {
        BookingResponse booking = bookings.create(
                BookingTestFixtures.request(CHECK_IN.plusDays(seq * 5L), CHECK_OUT.plusDays(seq * 5L), 1, seq),
                null, "127.0.0.1", "junit");
        Long id = bookingRepository.findByCode(booking.code()).orElseThrow().getId();
        for (BookingStatus target : List.of(
                BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT)) {
            stateMachine.transition(
                    bookingRepository.findById(id).orElseThrow(), target, HistoryActor.ADMIN, "test");
        }
        return booking;
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> submit(String code, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Forwarded-For", clientIp);
        return rest.exchange(
                "http://localhost:" + port + "/api/bookings/" + code + "/review",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);
    }

    private int countReviews() {
        return jdbc.queryForObject("SELECT count(*) FROM reviews", Integer.class);
    }

    private String statusOfReview() {
        return jdbc.queryForObject("SELECT status FROM reviews", String.class);
    }
}
