package com.tvh.homestay.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import com.tvh.homestay.schema.AbstractPostgresIT;
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
 * Hết hạn giữ chỗ — và quan trọng hơn: những đơn KHÔNG được đụng vào.
 *
 * <p>Rút ngắn thời gian giữ chỗ bằng {@code properties} của
 * {@code @SpringBootTest} thay vì tạo một profile riêng: dự án chỉ có hai
 * profile {@code demo} và {@code prod}, và một profile thứ ba chỉ để test là
 * thêm một cấu hình không ai chạy thật.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "booking.hold-minutes=0",
            "booking.expiry-grace-minutes=0",
            // Tắt vòng quét tự động: test gọi thẳng runOnce() để kiểm soát thời
            // điểm, không phải chờ và đoán.
            "booking.expiry-scan-ms=3600000"
        })
class BookingExpiryIT extends AbstractPostgresIT {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 11, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 11, 4);

    @Autowired
    private BookingService bookings;

    @Autowired
    private BookingExpiryScheduler scheduler;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanSlate() {
        BookingTestFixtures.reset(jdbc, 2);
    }

    private BookingResponse createBooking(int seq) {
        return bookings.create(
                BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, seq), null, "127.0.0.1", "junit");
    }

    private String statusOf(String code) {
        return jdbc.queryForObject("SELECT status FROM bookings WHERE code = ?", String.class, code);
    }

    @Test
    @DisplayName("Đơn quá hạn và CHƯA nhận đồng nào → EXPIRED, phòng RELEASED, có dòng lịch sử SYSTEM")
    void expiredHoldReleasesRoomAndLeavesTrail() {
        BookingResponse booking = createBooking(1);

        assertThat(scheduler.runOnce()).isEqualTo(1);

        assertThat(statusOf(booking.code())).isEqualTo("EXPIRED");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM booking_rooms WHERE status = 'ACTIVE'", Integer.class))
                .as("phòng phải được trả lại ngay để khách khác đặt được")
                .isZero();

        List<Map<String, Object>> trail = jdbc.queryForList("""
                SELECT h.from_status, h.to_status, h.actor
                  FROM booking_status_history h
                  JOIN bookings b ON b.id = h.booking_id
                 WHERE b.code = ? AND h.to_status = 'EXPIRED'
                """, booking.code());
        assertThat(trail).hasSize(1);
        assertThat(trail.get(0)).containsEntry("from_status", "PENDING_PAYMENT")
                .containsEntry("actor", "SYSTEM");
    }

    @Test
    @DisplayName("Đơn ĐÃ nhận tiền không bao giờ bị bộ quét đụng vào")
    void bookingWithMoneyIsNeverExpired() {
        // Đây là kịch bản đắt giá nhất: khách chuyển tiền ở phút chót, webhook
        // về sau khi đã quá hạn. Không có điều kiện này thì kết cục là tiền
        // trong tài khoản, phòng đã bán cho người khác, và không ai biết.
        BookingResponse paid = createBooking(1);
        jdbc.update("""
                UPDATE payments SET amount_received = 100000
                 WHERE booking_id = (SELECT id FROM bookings WHERE code = ?)
                """, paid.code());

        BookingResponse unpaid = createBooking(2);

        assertThat(scheduler.runOnce()).as("chỉ đơn chưa nhận đồng nào bị hết hạn").isEqualTo(1);
        assertThat(statusOf(paid.code())).isEqualTo("PENDING_PAYMENT");
        assertThat(statusOf(unpaid.code())).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Đơn có webhook chưa xử lý xong cũng được tha")
    void bookingWithPendingWebhookIsNeverExpired() {
        BookingResponse booking = createBooking(1);
        jdbc.update("""
                INSERT INTO payment_webhook_events (provider, external_id, payment_id, payload,
                                                    processing_result, processed_at)
                SELECT 'SEPAY', 'evt-dang-cho', p.id, '{}'::jsonb, 'MATCHED', NULL
                  FROM payments p
                  JOIN bookings b ON b.id = p.booking_id
                 WHERE b.code = ?
                """, booking.code());

        assertThat(scheduler.runOnce()).isZero();
        assertThat(statusOf(booking.code())).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    @DisplayName("Hết hạn hoàn lại lượt khuyến mãi đã tiêu")
    void expiryReleasesPromotionUsage() {
        jdbc.update("""
                INSERT INTO promotions (id, code, name, discount_type, discount_value,
                                        starts_at, ends_at, usage_limit, used_count)
                VALUES (1, 'GIAM10', 'Giảm 10%', 'PERCENT', 10,
                        now() - interval '1 day', now() + interval '30 day', 10, 0)
                """);
        bookings.create(
                new com.tvh.homestay.booking.dto.BookingDtos.CreateBookingRequest(
                        BookingTestFixtures.ROOM_TYPE_ID, CHECK_IN, CHECK_OUT, 1, 2, 0,
                        "Khách Mã", "ma@example.com", "0900000099", null, "GIAM10"),
                null, "127.0.0.1", "junit");

        assertThat(jdbc.queryForObject("SELECT used_count FROM promotions WHERE id = 1", Integer.class))
                .isEqualTo(1);

        scheduler.runOnce();

        // Không hoàn lượt thì một mã 10 lượt bị đốt sạch bởi 10 người bấm đặt
        // rồi bỏ ngang trong 15 phút.
        assertThat(jdbc.queryForObject("SELECT used_count FROM promotions WHERE id = 1", Integer.class))
                .isZero();
    }
}
