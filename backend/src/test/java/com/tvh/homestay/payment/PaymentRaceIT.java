package com.tvh.homestay.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.booking.BookingExpiryScheduler;
import com.tvh.homestay.booking.BookingService;
import com.tvh.homestay.booking.BookingTestFixtures;
import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import com.tvh.homestay.payment.entity.WebhookProcessingResult;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Tiền về SAU khi đơn đã đóng — nhánh cứu tiền của khách.
 *
 * <p>Cửa sổ đua này có thật: ngân hàng → nhà cung cấp → API trễ 5–30 giây, còn
 * đồng hồ đếm ngược trên màn hình QR lại đẩy khách bấm chuyển khoản vào đúng
 * phút cuối. Điều KHÔNG bao giờ được phép xảy ra là: tiền đã vào tài khoản, đơn
 * ở {@code EXPIRED}, và không bản ghi nào cho thấy chuyện đó.
 *
 * <p>Giữ chỗ 0 phút và ân hạn 0 phút để đơn hết hạn ngay khi gọi bộ quét; vòng
 * quét tự động bị tắt để test làm chủ thời điểm thay vì chờ và đoán.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "SEPAY_ACCOUNT_NUMBER=0123456789",
            "SEPAY_BANK_CODE=MBBank",
            "booking.hold-minutes=0",
            "booking.expiry-grace-minutes=0",
            "booking.expiry-scan-ms=3600000",
            "mail.dispatch-ms=3600000"
        })
class PaymentRaceIT extends AbstractPostgresIT {

    private static final String RECEIVING_ACCOUNT = "0123456789";
    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 20);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 12, 22);

    @Autowired
    private BookingService bookings;

    @Autowired
    private SepayWebhookService webhooks;

    @Autowired
    private BookingExpiryScheduler expiry;

    @Autowired
    private JdbcTemplate jdbc;

    private static long nextEventId = 70_000;

    @BeforeEach
    void cleanSlate() {
        BookingTestFixtures.reset(jdbc, 2);
    }

    @Test
    @DisplayName("Tiền về sau khi đơn EXPIRED, còn phòng cùng loại → gán lại phòng và CONFIRMED")
    void lateMoneyRegainsRoomAndConfirms() {
        BookingResponse booking = createBooking(1);
        assertThat(expiry.runOnce()).isEqualTo(1);
        assertThat(statusOf(booking.code())).isEqualTo("EXPIRED");
        assertThat(activeRooms(booking.code()))
                .as("hết hạn phải nhả phòng, nếu không thì chẳng có gì để giành lại")
                .isZero();

        WebhookProcessingResult result = webhooks.handle(
                json(nextEventId++, RECEIVING_ACCOUNT, transferContent(booking), booking.depositAmount()));

        assertThat(result).isEqualTo(WebhookProcessingResult.LATE);
        assertThat(statusOf(booking.code())).isEqualTo("CONFIRMED");
        assertThat(activeRooms(booking.code()))
                .as("CONFIRMED mà không giữ phòng nào là đúng trạng thái thiết kế này sinh ra để ngăn")
                .isEqualTo(1);
        assertThat(amountReceived(transferContent(booking)))
                .isEqualByComparingTo(booking.depositAmount());
        // Đường đi bắt buộc: EXPIRED → AWAITING_REVIEW → CONFIRMED, và mỗi bước
        // để lại một dòng lịch sử với actor SYSTEM.
        assertThat(historyPath(booking.code()))
                .contains("EXPIRED>AWAITING_REVIEW", "AWAITING_REVIEW>CONFIRMED");
    }

    @Test
    @DisplayName("Tiền về sau khi đơn EXPIRED nhưng phòng đã bán → AWAITING_REVIEW + NEEDS_REVIEW, tiền vẫn được ghi nhận")
    void lateMoneyWithoutRoomGoesToManualReview() {
        // Chỉ một phòng: sau khi đơn đầu hết hạn, đơn thứ hai chiếm mất phòng đó.
        BookingTestFixtures.reset(jdbc, 1);
        BookingResponse abandoned = createBooking(1);
        assertThat(expiry.runOnce()).isEqualTo(1);

        BookingResponse taker = createBooking(2);
        assertThat(activeRooms(taker.code())).isEqualTo(1);

        WebhookProcessingResult result = webhooks.handle(json(
                nextEventId++, RECEIVING_ACCOUNT, transferContent(abandoned), abandoned.depositAmount()));

        assertThat(result).isEqualTo(WebhookProcessingResult.LATE);
        assertThat(statusOf(abandoned.code()))
                .as("hết phòng thì dừng ở AWAITING_REVIEW cho người xử lý, không tự hoàn tiền")
                .isEqualTo("AWAITING_REVIEW");
        assertThat(paymentColumn(transferContent(abandoned), "reconcile_status"))
                .isEqualTo("NEEDS_REVIEW");
        assertThat(amountReceived(transferContent(abandoned)))
                .as("tiền đã vào tài khoản thì phải được ghi nhận, kể cả khi không còn phòng")
                .isEqualByComparingTo(abandoned.depositAmount());
        assertThat(statusOf(taker.code()))
                .as("đơn của khách sau không được đụng tới")
                .isEqualTo("PENDING_PAYMENT");
    }

    /**
     * Câu hỏi mà Phase 6 tự đặt ra cho chính mình: sau khi gia hạn giữ chỗ +24h
     * cho đơn thiếu tiền, còn nhánh nào khiến một đơn ĐÃ CÓ TIỀN bị chuyển
     * {@code EXPIRED} không?
     */
    @Test
    @DisplayName("Đơn đã nhận tiền (một phần) không bao giờ bị bộ quét cho hết hạn")
    void bookingWithMoneyIsNeverExpiredByScanner() {
        BookingResponse booking = createBooking(1);

        webhooks.handle(json(nextEventId++, RECEIVING_ACCOUNT, transferContent(booking),
                booking.depositAmount().subtract(new BigDecimal("100000"))));
        assertThat(statusOf(booking.code())).isEqualTo("AWAITING_REVIEW");

        // Chạy bộ quét ba lần, kể cả sau khi ép hạn giữ chỗ về quá khứ: không
        // lần nào được phép đụng vào đơn đã có tiền.
        assertThat(expiry.runOnce()).isZero();
        jdbc.update("UPDATE bookings SET hold_expires_at = now() - interval '2 days' WHERE code = ?",
                booking.code());
        assertThat(expiry.runOnce()).isZero();
        assertThat(expiry.runOnce()).isZero();

        assertThat(statusOf(booking.code())).isEqualTo("AWAITING_REVIEW");
        assertThat(amountReceived(transferContent(booking))).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Đơn PENDING_PAYMENT đã nhận một phần tiền cũng không bị bộ quét cho hết hạn")
    void pendingBookingWithMoneySurvivesScanner() {
        BookingResponse booking = createBooking(1);
        // Dựng thẳng bằng SQL đúng cái trạng thái trung gian mà cửa sổ đua tạo
        // ra: tiền đã ghi nhận nhưng đơn chưa kịp đổi trạng thái.
        jdbc.update("UPDATE payments SET amount_received = 1000 WHERE transfer_content = ?",
                transferContent(booking));

        assertThat(expiry.runOnce()).isZero();
        assertThat(statusOf(booking.code())).isEqualTo("PENDING_PAYMENT");
    }

    // ─────────────────────────────────────────────────────────────────────

    private BookingResponse createBooking(int seq) {
        return bookings.create(
                BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, seq), null, "127.0.0.1", "junit");
    }

    private static String transferContent(BookingResponse booking) {
        return booking.payment().transferContent();
    }

    private static String json(long id, String account, String content, BigDecimal amount) {
        return """
                {"id":%d,"gateway":"TestBank","transactionDate":"2026-12-19 23:59:00",
                 "accountNumber":"%s","content":"%s","transferType":"in",
                 "transferAmount":%s,"referenceCode":"REF%d"}
                """.formatted(id, account, content, amount.toPlainString(), id);
    }

    private String statusOf(String code) {
        return jdbc.queryForObject("SELECT status FROM bookings WHERE code = ?", String.class, code);
    }

    private int activeRooms(String code) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM booking_rooms br
                JOIN bookings b ON b.id = br.booking_id
                WHERE b.code = ? AND br.status = 'ACTIVE'
                """, Integer.class, code);
    }

    private BigDecimal amountReceived(String transferContent) {
        return jdbc.queryForObject(
                "SELECT amount_received FROM payments WHERE transfer_content = ?",
                BigDecimal.class, transferContent);
    }

    private String paymentColumn(String transferContent, String column) {
        return jdbc.queryForObject(
                "SELECT " + column + " FROM payments WHERE transfer_content = ?",
                String.class, transferContent);
    }

    private String historyPath(String code) {
        return String.join(",", jdbc.queryForList("""
                SELECT coalesce(h.from_status, '-') || '>' || h.to_status
                FROM booking_status_history h
                JOIN bookings b ON b.id = h.booking_id
                WHERE b.code = ? AND h.actor = 'SYSTEM'
                ORDER BY h.id
                """, String.class, code));
    }
}
