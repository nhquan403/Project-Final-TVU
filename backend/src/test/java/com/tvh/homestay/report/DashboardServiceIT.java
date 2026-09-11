package com.tvh.homestay.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.report.dto.DashboardSummary.MonthlyValue;
import com.tvh.homestay.report.dto.DashboardSummary.Summary;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Số liệu dashboard, và bốn cách nó dễ sai mà vẫn trông hợp lý.
 *
 * <p>Dữ liệu nền dựng bằng SQL thô: các kiểm tra ở đây nói về cách TỔNG HỢP,
 * nên dựng nền bằng chính tầng đang được kiểm là tự bịt mắt. Mỗi đơn giữ đúng
 * một phòng để bất biến {@code room_quantity = số dòng ACTIVE} luôn đúng ngay
 * sau từng câu lệnh (JdbcTemplate tự commit từng câu).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "booking.expiry-scan-ms=3600000")
class DashboardServiceIT extends AbstractPostgresIT {

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2027, 1, 1);

    @Autowired
    private DashboardService dashboard;

    @Autowired
    private JdbcTemplate jdbc;

    private int sequence;

    @BeforeEach
    void cleanSlate() {
        jdbc.execute("DELETE FROM booking_notes");
        jdbc.execute("DELETE FROM booking_status_history");
        jdbc.execute("DELETE FROM outbound_emails");
        jdbc.execute("DELETE FROM payment_webhook_events");
        jdbc.execute("DELETE FROM payments");
        jdbc.execute("DELETE FROM bookings");
        jdbc.execute("DELETE FROM rooms");
        jdbc.execute("DELETE FROM room_types");
        jdbc.update("""
                INSERT INTO room_types (id, code, slug, name, base_price,
                                        capacity_adults, capacity_children, bed_info)
                VALUES (1, 'GARDEN', 'phong-vuon', 'Phòng Vườn', 500000, 2, 2, '1 giường đôi')
                """);
        jdbc.update("INSERT INTO rooms (id, room_type_id, room_number, status)"
                + " VALUES (1, 1, '101', 'AVAILABLE'), (2, 1, '102', 'AVAILABLE')");
        sequence = 0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 1. Khớp SQL thô, CÙNG tham số kỳ
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Giá trị booking theo tháng khớp truy vấn SQL thô khi truyền CÙNG kỳ")
    void bookingValueMatchesRawSql() {
        insertBooking(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 7), "CONFIRMED", "1000000", 1);
        insertBooking(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 22), "CHECKED_OUT", "1500000", 2);
        insertBooking(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), "CHECKED_IN", "500000", 1);
        // Đơn đã huỷ KHÔNG được tính vào giá trị booking.
        insertBooking(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11), "CANCELLED", "900000", null);

        Summary summary = dashboard.summary(PERIOD_START, PERIOD_END);

        List<Map<String, Object>> raw = jdbc.queryForList("""
                SELECT date_trunc('month', check_in)::date AS thang, SUM(total_amount) AS tong
                  FROM bookings
                 WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')
                   AND check_in >= ? AND check_in < ?
                 GROUP BY 1 ORDER BY 1
                """, PERIOD_START, PERIOD_END);

        for (Map<String, Object> row : raw) {
            LocalDate month = ((java.sql.Date) row.get("thang")).toLocalDate();
            BigDecimal expected = (BigDecimal) row.get("tong");
            assertThat(valueOf(summary.bookingValueByMonth(), month))
                    .as("tháng %s", month)
                    .isEqualByComparingTo(expected);
        }
        assertThat(summary.bookingValueTotal()).isEqualByComparingTo("3000000");
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2. Phòng chuyển bảo trì không được làm đổi số liệu lịch sử
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Đưa phòng có booking lịch sử sang MAINTENANCE không làm đổi tỉ lệ lấp đầy tháng cũ")
    void maintenanceDoesNotChangeHistoricalOccupancy() {
        insertBooking(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 11), "CHECKED_OUT", "5000000", 1);

        List<MonthlyValue> before = dashboard.summary(PERIOD_START, PERIOD_END).occupancyByMonth();
        jdbc.update("UPDATE rooms SET status = 'MAINTENANCE' WHERE id = 1");
        List<MonthlyValue> after = dashboard.summary(PERIOD_START, PERIOD_END).occupancyByMonth();

        LocalDate february = LocalDate.of(2026, 2, 1);
        // 10 đêm-phòng / (28 ngày × 2 phòng) — mẫu số là TỔNG SỐ PHÒNG VẬT LÝ.
        // Lọc mẫu số theo status='AVAILABLE' sẽ biến con số này thành 10/28 sau
        // khi phòng 101 đi bảo trì: số liệu của một tháng đã qua tự nhiên nhảy
        // gần gấp đôi chỉ vì hôm nay ai đó sửa một dòng ở bảng rooms.
        assertThat(rounded(valueOf(before, february)))
                .isEqualByComparingTo(new BigDecimal("10").divide(
                        new BigDecimal("56"), 6, RoundingMode.HALF_UP));
        assertThat(valueOf(after, february))
                .as("số liệu lịch sử không được đổi khi trạng thái phòng hôm nay đổi")
                .isEqualByComparingTo(valueOf(before, february));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3. Đơn vắt qua hai tháng
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Đơn vắt qua ranh giới tháng chỉ tính phần nằm trong từng tháng, không vượt 100%")
    void bookingSpanningTwoMonthsIsSplitAndNeverExceedsFull() {
        // 30/01 → 02/02: hai đêm thuộc tháng 1 (30, 31), một đêm thuộc tháng 2.
        insertBooking(LocalDate.of(2026, 1, 30), LocalDate.of(2026, 2, 2), "CONFIRMED", "1500000", 1);

        List<MonthlyValue> occupancy = dashboard.summary(PERIOD_START, PERIOD_END).occupancyByMonth();

        assertThat(rounded(valueOf(occupancy, LocalDate.of(2026, 1, 1))))
                .isEqualByComparingTo(new BigDecimal("2").divide(
                        new BigDecimal("62"), 6, RoundingMode.HALF_UP));
        assertThat(rounded(valueOf(occupancy, LocalDate.of(2026, 2, 1))))
                .isEqualByComparingTo(new BigDecimal("1").divide(
                        new BigDecimal("56"), 6, RoundingMode.HALF_UP));
        assertThat(occupancy).allSatisfy(month ->
                assertThat(month.value())
                        .as("tỉ lệ lấp đầy không bao giờ vượt 100%%")
                        .isLessThanOrEqualTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("Bán kín mọi phòng cả tháng cho ra đúng 100%, không hơn")
    void fullyBookedMonthIsExactlyOne() {
        insertBooking(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), "CONFIRMED", "15000000", 1);
        insertBooking(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), "CONFIRMED", "15000000", 2);

        List<MonthlyValue> occupancy = dashboard.summary(PERIOD_START, PERIOD_END).occupancyByMonth();

        assertThat(valueOf(occupancy, LocalDate.of(2026, 6, 1))).isEqualByComparingTo(BigDecimal.ONE);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4. Múi giờ
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Đơn tạo lúc 01:00 giờ Việt Nam ngày mùng 1 được xếp vào ĐÚNG tháng đó")
    void bookingCreatedJustAfterMidnightVietnamTimeLandsInTheRightMonth() {
        insertBooking(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12), "CONFIRMED", "1000000", 1);
        jdbc.update("UPDATE bookings SET created_at = TIMESTAMPTZ '2026-03-01 01:00:00+07'");

        Summary summary = dashboard.summary(PERIOD_START, PERIOD_END);

        assertThat(valueOf(summary.newBookingsByMonth(), LocalDate.of(2026, 3, 1)))
                .as("01:00 ngày 01/03 giờ Việt Nam là tháng 3")
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(valueOf(summary.newBookingsByMonth(), LocalDate.of(2026, 2, 1)))
                .isEqualByComparingTo(BigDecimal.ZERO);

        // Bằng chứng mệnh đề AT TIME ZONE là thứ có tác dụng, không phải may:
        // cùng dòng dữ liệu đó, gom nhóm theo giờ UTC sẽ rơi vào tháng 2.
        LocalDate utcMonth = jdbc.queryForObject(
                "SELECT date_trunc('month', (created_at AT TIME ZONE 'UTC'))::date FROM bookings",
                java.sql.Date.class).toLocalDate();
        assertThat(utcMonth)
                .as("gom nhóm theo UTC đẩy đơn này về tháng trước — đó là lỗi cần tránh")
                .isEqualTo(LocalDate.of(2026, 2, 1));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 5. Badge "cần đối soát"
    // ─────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Badge cần đối soát chỉ đếm khoản CHƯA xử lý, không đếm khoản đã RESOLVED")
    void reconcileBadgeCountsOnlyOutstandingWork() {
        insertBooking(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3), "AWAITING_REVIEW", "1000000", 1);
        insertBooking(LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 7), "CONFIRMED", "1000000", 2);
        insertPayment("TVHDS0001", "NEEDS_REVIEW");
        insertPayment("TVHDS0002", "RESOLVED");

        assertThat(dashboard.summary(PERIOD_START, PERIOD_END).reconcileCount())
                .as("đếm cả khoản đã xử lý thì con số này không bao giờ giảm, "
                        + "và một badge chỉ tăng là badge người dùng học cách phớt lờ")
                .isEqualTo(1);
    }

    private void insertPayment(String bookingCode, String reconcileStatus) {
        jdbc.update("""
                INSERT INTO payments (booking_id, attempt_no, provider, amount_expected,
                                      amount_received, transfer_content, status, reconcile_status)
                SELECT id, 1, 'SEPAY', 300000, 100000, ? , 'PARTIAL', ?
                  FROM bookings WHERE code = ?
                """, bookingCode + "01", reconcileStatus, bookingCode);
    }

    /** SQL trả tỉ lệ với toàn bộ chữ số; so sánh ở mức 6 chữ số thập phân là đủ. */
    private static BigDecimal rounded(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal valueOf(List<MonthlyValue> series, LocalDate month) {
        return series.stream()
                .filter(entry -> entry.month().equals(month))
                .map(MonthlyValue::value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có dữ liệu tháng " + month));
    }

    /** Một đơn giữ ĐÚNG MỘT phòng; {@code roomId} null nghĩa là đơn đã đóng, không giữ phòng. */
    private void insertBooking(
            LocalDate checkIn, LocalDate checkOut, String status, String total, Integer roomId) {
        BigDecimal amount = new BigDecimal(total);
        sequence++;
        String code = "TVHDS%04d".formatted(sequence);
        jdbc.update("""
                INSERT INTO bookings (code, access_token, guest_name, guest_email, guest_phone,
                                      check_in, check_out, adults, room_type_id,
                                      room_type_name_snapshot, unit_price_snapshot, room_quantity,
                                      subtotal_amount, total_amount, deposit_amount,
                                      status, payment_status)
                VALUES (?, ?, ?, ?, '0900000000', ?, ?, 2, 1, 'Phòng Vườn', 500000, 1,
                        ?, ?, 0, ?, 'UNPAID')
                """,
                code, "%032d".formatted(sequence), "Khách " + sequence,
                "kh" + sequence + "@example.com", checkIn, checkOut, amount, amount, status);

        if (roomId != null) {
            jdbc.update("""
                    INSERT INTO booking_rooms (booking_id, room_id, check_in, check_out, status)
                    SELECT id, ?, ?, ?, 'ACTIVE' FROM bookings WHERE code = ?
                    """, roomId, checkIn, checkOut, code);
        }
    }
}
