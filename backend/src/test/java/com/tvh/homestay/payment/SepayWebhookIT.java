package com.tvh.homestay.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.booking.BookingService;
import com.tvh.homestay.booking.BookingTestFixtures;
import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * Webhook SePay: năm lớp kiểm tra và bảng quyết định.
 *
 * <p>Đi qua cổng HTTP thật vì phần lớn giá trị của các kiểm tra này nằm ở lớp
 * biên: mã trạng thái, thân phản hồi {@code {"success": true}}, và cách so khoá
 * xác thực. Gọi thẳng vào service sẽ bỏ qua đúng những thứ đó.
 *
 * <p>Số tài khoản nhận được đặt bằng {@code properties} của
 * {@code @SpringBootTest}: nếu không cấu hình, lớp kiểm tra tài khoản đích sẽ
 * từ chối MỌI giao dịch (fail-closed) và test số 6 sẽ xanh vì lý do sai.
 *
 * <p>Khoá webhook KHÔNG ghi cứng ở đây — {@code AbstractPostgresIT} sinh ngẫu
 * nhiên mỗi lần chạy và test đọc lại qua {@code @Value}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "SEPAY_ACCOUNT_NUMBER=0123456789",
            "SEPAY_BANK_CODE=MBBank",
            // Tắt mọi bộ quét nền: test tự quyết định thời điểm, không chờ và đoán.
            "booking.expiry-scan-ms=3600000",
            "mail.dispatch-ms=3600000"
        })
class SepayWebhookIT extends AbstractPostgresIT {

    private static final String RECEIVING_ACCOUNT = "0123456789";
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 10);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 12);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private BookingService bookings;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private com.tvh.homestay.mail.EmailDispatchScheduler dispatcher;

    @Value("${SEPAY_WEBHOOK_API_KEY}")
    private String apiKey;

    private static long nextEventId = 90_000;

    @BeforeEach
    void cleanSlate() {
        BookingTestFixtures.reset(jdbc, 2);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 1. Đúng khoá + đủ tiền
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Đúng khoá, đủ tiền → 200 {\"success\": true}, đơn CONFIRMED, có thư trong hộp thư đi")
    void correctKeyAndFullAmountConfirmsBooking() {
        BookingResponse booking = createBooking(1);
        long eventId = nextEventId++;

        ResponseEntity<Map> response = postWebhook(
                json(eventId, "in", RECEIVING_ACCOUNT, transferContent(booking), deposit(booking)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("SePay chỉ coi là giao thành công khi thấy đúng thân này")
                .isEqualTo(Map.of("success", true));

        assertThat(statusOf(booking.code())).isEqualTo("CONFIRMED");
        assertThat(paymentStatusOf(booking.code())).isEqualTo("DEPOSIT_PAID");
        assertThat(paymentColumn(transferContent(booking), "status")).isEqualTo("SUCCEEDED");
        assertThat(amountReceived(transferContent(booking))).isEqualByComparingTo(deposit(booking));
        assertThat(eventResult(eventId)).isEqualTo("MATCHED");
        assertThat(emailCount(booking.code(), "booking-confirmed")).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Gọi lại cùng external_id
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Gọi lại cùng external_id → 200, tiền KHÔNG cộng thêm, không sinh thư thứ hai")
    void sameExternalIdIsIgnored() {
        BookingResponse booking = createBooking(2);
        long eventId = nextEventId++;
        String body = json(eventId, "in", RECEIVING_ACCOUNT, transferContent(booking), deposit(booking));

        postWebhook(body);
        ResponseEntity<Map> second = postWebhook(body);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isEqualTo(Map.of("success", true));
        assertThat(amountReceived(transferContent(booking)))
                .as("cộng dồn hai lần cùng một giao dịch là ghi nhận tiền không có thật")
                .isEqualByComparingTo(deposit(booking));
        assertThat(emailCount(booking.code(), "booking-confirmed")).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM payment_webhook_events WHERE external_id = ?",
                        Integer.class, String.valueOf(eventId)))
                .isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. Lần trước ERROR → xử lý lại
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Sự kiện lần trước ERROR → lần gửi lại được XỬ LÝ LẠI, không bị khoá vĩnh viễn")
    void previousErrorIsReprocessed() {
        BookingResponse booking = createBooking(3);
        long eventId = nextEventId++;

        // Dựng đúng hiện trường của một lần hỏng giữa chừng: dòng sự kiện đã có,
        // mang ERROR, chưa processed_at, và chưa đồng nào được ghi nhận.
        jdbc.update("""
                INSERT INTO payment_webhook_events
                       (provider, external_id, payload, processing_result, error_message, received_at)
                VALUES ('SEPAY', ?, '{}'::jsonb, 'ERROR', 'mat ket noi', now())
                """, String.valueOf(eventId));

        postWebhook(json(eventId, "in", RECEIVING_ACCOUNT, transferContent(booking), deposit(booking)));

        assertThat(statusOf(booking.code()))
                .as("khoá chống trùng phải phân biệt ERROR, nếu không tiền của khách bị khoá vĩnh viễn")
                .isEqualTo("CONFIRMED");
        assertThat(eventResult(eventId)).isEqualTo("MATCHED");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4. Sai khoá
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Sai khoá → 401 và KHÔNG ghi sự kiện nào")
    void wrongApiKeyIsRejected() {
        BookingResponse booking = createBooking(4);
        long eventId = nextEventId++;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, "Apikey khoa-sai-hoan-toan");
        ResponseEntity<Map> response = rest.exchange(
                url(),
                HttpMethod.POST,
                new HttpEntity<>(
                        json(eventId, "in", RECEIVING_ACCOUNT, transferContent(booking), deposit(booking)),
                        headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(statusOf(booking.code())).isEqualTo("PENDING_PAYMENT");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM payment_webhook_events", Integer.class))
                .isZero();
    }

    // ─────────────────────────────────────────────────────────────────────
    // 5. Chiều tiền sai
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("transferType='out' → 200 nhưng UNMATCHED, đơn KHÔNG được xác nhận")
    void outgoingTransferNeverConfirms() {
        BookingResponse booking = createBooking(5);
        long eventId = nextEventId++;

        ResponseEntity<Map> response = postWebhook(
                json(eventId, "out", RECEIVING_ACCOUNT, transferContent(booking), deposit(booking)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusOf(booking.code()))
                .as("tiền đi RA khỏi tài khoản không bao giờ được xác nhận một đơn")
                .isEqualTo("PENDING_PAYMENT");
        assertThat(amountReceived(transferContent(booking))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(eventResult(eventId)).isEqualTo("UNMATCHED");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 6. Tài khoản đích lạ
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Tiền vào tài khoản khác → UNMATCHED, đơn KHÔNG được xác nhận")
    void unknownReceivingAccountNeverConfirms() {
        BookingResponse booking = createBooking(6);
        long eventId = nextEventId++;

        postWebhook(json(eventId, "in", "9999999999", transferContent(booking), deposit(booking)));

        assertThat(statusOf(booking.code())).isEqualTo("PENDING_PAYMENT");
        assertThat(eventResult(eventId)).isEqualTo("UNMATCHED");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 7. Nội dung chứa hai mã
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Nội dung chứa hai mã đơn → UNMATCHED, không đoán bừa mã nào")
    void twoCodesInContentGoesToReconcileQueue() {
        BookingResponse first = createBooking(7);
        BookingResponse second = createBooking(8);
        long eventId = nextEventId++;

        postWebhook(json(eventId, "in", RECEIVING_ACCOUNT,
                transferContent(first) + " " + transferContent(second), deposit(first)));

        assertThat(eventResult(eventId)).isEqualTo("UNMATCHED");
        assertThat(statusOf(first.code())).isEqualTo("PENDING_PAYMENT");
        assertThat(statusOf(second.code())).isEqualTo("PENDING_PAYMENT");
        assertThat(amountReceived(transferContent(first))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 8. Thiếu tiền
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Chuyển thiếu tiền → AWAITING_REVIEW + PARTIAL + gia hạn giữ chỗ, có thư nhắc")
    void partialPaymentExtendsHoldAndQueuesReview() {
        BookingResponse booking = createBooking(9);
        long eventId = nextEventId++;
        java.time.OffsetDateTime holdBefore = holdExpiresAt(booking.code());

        // Thiếu hẳn 100.000đ — ngoài dung sai ±1.000đ, nên phải rơi vào PARTIAL.
        postWebhook(json(eventId, "in", RECEIVING_ACCOUNT, transferContent(booking),
                deposit(booking).subtract(new BigDecimal("100000"))));

        assertThat(statusOf(booking.code())).isEqualTo("AWAITING_REVIEW");
        assertThat(paymentStatusOf(booking.code())).isEqualTo("PARTIAL");
        assertThat(paymentColumn(transferContent(booking), "status")).isEqualTo("PARTIAL");
        assertThat(paymentColumn(transferContent(booking), "reconcile_status"))
                .as("thiếu tiền phải hiện ra ở hàng đợi đối soát, không biến mất")
                .isEqualTo("NEEDS_REVIEW");
        assertThat(holdExpiresAt(booking.code()))
                .as("gia hạn giữ chỗ để khách kịp bù phần còn thiếu")
                .isAfter(holdBefore.plusHours(20));
        assertThat(emailCount(booking.code(), "booking-partial")).isEqualTo(1);
        assertThat(eventResult(eventId)).isEqualTo("MATCHED");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 9. Thiếu rồi bù cho đủ
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Chuyển thiếu rồi bù → cộng dồn đủ tiền → CONFIRMED")
    void partialThenToppedUpConfirms() {
        BookingResponse booking = createBooking(10);
        BigDecimal half = deposit(booking).divide(new BigDecimal("2"));

        postWebhook(json(nextEventId++, "in", RECEIVING_ACCOUNT, transferContent(booking), half));
        assertThat(statusOf(booking.code())).isEqualTo("AWAITING_REVIEW");

        postWebhook(json(nextEventId++, "in", RECEIVING_ACCOUNT, transferContent(booking),
                deposit(booking).subtract(half)));

        assertThat(statusOf(booking.code()))
                .as("amount_received phải CỘNG DỒN, ghi đè thì lần chuyển đầu biến mất")
                .isEqualTo("CONFIRMED");
        assertThat(amountReceived(transferContent(booking))).isEqualByComparingTo(deposit(booking));
        assertThat(emailCount(booking.code(), "booking-confirmed")).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 10. Không có SMTP thì hộp thư đi vẫn phải chứng minh được là đã gửi
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Không cấu hình MAIL_HOST → thư vẫn được gửi qua đường ghi log và outbox chuyển SENT")
    void outboxIsDispatchedEvenWithoutSmtp() {
        BookingResponse booking = createBooking(11);
        postWebhook(json(nextEventId++, "in", RECEIVING_ACCOUNT,
                transferContent(booking), deposit(booking)));
        assertThat(emailStatus(booking.code())).isEqualTo("PENDING");

        // Test chạy không có MAIL_HOST, nên đường ra là LoggingMailSender. Nó
        // KHÔNG phải một cái mock để cho qua chuyện: dòng outbox vẫn phải
        // chuyển SENT, vì đó là thứ chứng minh "đã gửi" bằng SQL thay vì bằng
        // mắt nhìn hộp thư.
        assertThat(dispatcher.runOnce()).isEqualTo(1);
        assertThat(emailStatus(booking.code())).isEqualTo("SENT");
        assertThat(dispatcher.runOnce())
                .as("thư đã gửi không được gửi lại ở vòng quét sau")
                .isZero();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Trợ giúp
    // ─────────────────────────────────────────────────────────────────────

    private BookingResponse createBooking(int seq) {
        return bookings.create(
                BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, seq), null, "127.0.0.1", "junit");
    }

    private static String transferContent(BookingResponse booking) {
        return booking.payment().transferContent();
    }

    private static BigDecimal deposit(BookingResponse booking) {
        return booking.depositAmount();
    }

    private String url() {
        return "http://localhost:" + port + "/api/payments/webhook/sepay";
    }

    private ResponseEntity<Map> postWebhook(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.AUTHORIZATION, "Apikey " + apiKey);
        return rest.exchange(url(), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    private static String json(
            long id, String transferType, String account, String content, BigDecimal amount) {
        return """
                {"id":%d,"gateway":"TestBank","transactionDate":"2026-12-09 10:00:00",
                 "accountNumber":"%s","subAccount":null,"code":null,"content":"%s",
                 "transferType":"%s","description":"CK","transferAmount":%s,
                 "accumulated":0,"referenceCode":"REF%d"}
                """.formatted(id, account, content, transferType, amount.toPlainString(), id);
    }

    private String statusOf(String code) {
        return jdbc.queryForObject("SELECT status FROM bookings WHERE code = ?", String.class, code);
    }

    private String paymentStatusOf(String code) {
        return jdbc.queryForObject(
                "SELECT payment_status FROM bookings WHERE code = ?", String.class, code);
    }

    private java.time.OffsetDateTime holdExpiresAt(String code) {
        return jdbc.queryForObject(
                "SELECT hold_expires_at FROM bookings WHERE code = ?",
                java.time.OffsetDateTime.class, code);
    }

    private String paymentColumn(String transferContent, String column) {
        return jdbc.queryForObject(
                "SELECT " + column + " FROM payments WHERE transfer_content = ?",
                String.class, transferContent);
    }

    private BigDecimal amountReceived(String transferContent) {
        return jdbc.queryForObject(
                "SELECT amount_received FROM payments WHERE transfer_content = ?",
                BigDecimal.class, transferContent);
    }

    private String eventResult(long externalId) {
        return jdbc.queryForObject(
                "SELECT processing_result FROM payment_webhook_events WHERE external_id = ?",
                String.class, String.valueOf(externalId));
    }

    private String emailStatus(String bookingCode) {
        return jdbc.queryForObject("""
                SELECT e.status FROM outbound_emails e
                JOIN bookings b ON b.id = e.booking_id
                WHERE b.code = ?
                """, String.class, bookingCode);
    }

    private int emailCount(String bookingCode, String template) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM outbound_emails e
                JOIN bookings b ON b.id = e.booking_id
                WHERE b.code = ? AND e.template = ?
                """, Integer.class, bookingCode, template);
    }
}
