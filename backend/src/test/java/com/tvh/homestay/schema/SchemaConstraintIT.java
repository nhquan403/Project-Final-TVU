package com.tvh.homestay.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.payment.entity.OutboundEmail;
import com.tvh.homestay.payment.repository.OutboundEmailRepository;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mỗi ràng buộc sống còn một test.
 *
 * <p>Test ở đây cố ý đi bằng SQL thô chứ không qua JPA: thứ cần chứng minh là
 * CƠ SỞ DỮ LIỆU tự nó chặn, kể cả khi có ai đó ghi thẳng bằng psql hay bằng
 * một service viết ẩu ở phase sau. Ràng buộc chỉ tồn tại ở tầng Java thì
 * không phải ràng buộc, chỉ là quy ước.
 *
 * <p>KHÔNG dùng {@code @Transactional} cho lớp này: hai trong năm ràng buộc
 * chỉ bắn lúc COMMIT, mà test tự rollback thì không bao giờ có commit nào để
 * bắn.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SchemaConstraintIT extends AbstractPostgresIT {

    /** SQLSTATE của vi phạm ràng buộc EXCLUDE. */
    private static final String EXCLUSION_VIOLATION = "23P01";
    /** SQLSTATE của vi phạm CHECK. */
    private static final String CHECK_VIOLATION = "23514";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private OutboundEmailRepository emails;

    private TransactionTemplate tx;

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(transactionManager);

        // Xoá bookings TRƯỚC, để khoá ngoại ON DELETE CASCADE cuốn theo
        // booking_rooms trong cùng một transaction. Xoá booking_rooms riêng lẻ
        // sẽ để lại đơn có room_quantity = 1 mà không còn dòng ACTIVE nào —
        // đúng cái bất biến mà trigger sinh ra để chặn, nên chính bước dọn dẹp
        // sẽ hỏng. Đây là lần đầu tiên bất biến đó bắt được lỗi thật, và nó
        // bắt lỗi của test.
        jdbc.execute("DELETE FROM outbound_emails");
        jdbc.execute("DELETE FROM bookings");
        jdbc.execute("DELETE FROM rooms");
        jdbc.execute("DELETE FROM room_types");
        jdbc.execute("DELETE FROM promotions");

        jdbc.update("""
                INSERT INTO room_types (id, code, slug, name, base_price, capacity_adults)
                VALUES (1, 'GARDEN', 'phong-vuon', 'Phòng Vườn', 850000, 2)
                """);
        jdbc.update("INSERT INTO rooms (id, room_type_id, room_number) VALUES (1, 1, '101')");
        for (int n = 1; n <= 4; n++) {
            insertBooking(n, 1);
        }
    }

    // ── a. Chồng ngày cùng một phòng ───────────────────────────────────────
    @Test
    @DisplayName("a. Hai khoảng chồng nhau trên cùng một phòng bị bác với 23P01")
    void overlappingStaysAreRejected() {
        assignRoom(1, "2026-10-01", "2026-10-05");

        assertThatThrownBy(() -> assignRoom(2, "2026-10-03", "2026-10-07"))
                .satisfies(thrown -> assertThat(sqlState(thrown)).isEqualTo(EXCLUSION_VIOLATION));
    }

    // ── b. Khoảng nửa mở liền kề ───────────────────────────────────────────
    @Test
    @DisplayName("b. Trả phòng 10-05 và nhận phòng 10-05 KHÔNG chồng nhau")
    void adjacentHalfOpenRangesAreAllowed() {
        assignRoom(1, "2026-10-01", "2026-10-05");

        // Đúng nghiệp vụ khách sạn: khách A đi buổi sáng, khách B đến buổi
        // chiều cùng ngày. Quy ước [) làm điều này đúng mà không cần mã nào.
        assertThatCode(() -> assignRoom(2, "2026-10-05", "2026-10-08")).doesNotThrowAnyException();
    }

    // ── c. Sai chính tả trạng thái ─────────────────────────────────────────
    @Test
    @DisplayName("c. Ghi 'Active' bị CHECK chặn, không lọt xuống bảng")
    void misspelledStatusCannotReachTheTable() {
        // Đây là lỗ hổng nguy hiểm nhất nếu thiếu CHECK: 'Active' nằm ngoài
        // mệnh đề WHERE (status = 'ACTIVE') của ràng buộc EXCLUDE, nên phòng
        // sẽ bị bán trùng ÂM THẦM — không lỗi, không log.
        assignRoom(1, "2026-10-01", "2026-10-05");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO booking_rooms (booking_id, room_id, check_in, check_out, status)
                VALUES (2, 1, DATE '2026-10-02', DATE '2026-10-04', 'Active')
                """))
                .satisfies(thrown -> assertThat(sqlState(thrown)).isEqualTo(CHECK_VIOLATION));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM booking_rooms", Integer.class)).isEqualTo(1);
    }

    // ── d. Bất biến room_quantity ↔ số dòng ACTIVE ─────────────────────────
    @Test
    @DisplayName("d. room_quantity lệch số dòng ACTIVE bị chặn lúc COMMIT")
    void roomQuantityInvariantIsEnforcedAtCommit() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE bookings SET room_quantity = 2 WHERE id = 1");
            assignRoom(1, "2026-10-01", "2026-10-05"); // mới có 1 dòng, cần 2
        })).satisfies(thrown -> assertThat(rootMessage(thrown))
                .contains("booking_rooms khong khop room_quantity"));
    }

    @Test
    @DisplayName("d2. Trigger DEFERRED: bất biến được phép TẠM sai giữa transaction")
    void invariantMayBeTemporarilyViolatedInsideTransaction() {
        // Không có DEFERRABLE INITIALLY DEFERRED thì dòng phòng đầu tiên đã
        // làm trigger bắn, và không đơn nhiều phòng nào đặt được.
        jdbc.update("INSERT INTO rooms (id, room_type_id, room_number) VALUES (2, 1, '102')");

        assertThatCode(() -> tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE bookings SET room_quantity = 2 WHERE id = 1");
            assignRoom(1, "2026-10-01", "2026-10-05");
            assignRoomTo(1, 2, "2026-10-01", "2026-10-05");
        })).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("d3. Không sửa được room_quantity từ phía bookings mà bỏ qua bất biến")
    void invariantCannotBeBypassedFromTheBookingSide() {
        assignRoom(1, "2026-10-01", "2026-10-05");

        // Trigger đặt trên booking_rooms không thấy lệnh này. Nếu chỉ có một
        // phía được canh, đây là đường vòng để tạo ra đúng cái trạng thái mà
        // bất biến sinh ra để ngăn: đơn thu tiền 9 phòng nhưng chỉ giữ 1.
        assertThatThrownBy(() -> jdbc.update("UPDATE bookings SET room_quantity = 9 WHERE id = 1"))
                .satisfies(thrown -> assertThat(rootMessage(thrown))
                        .contains("booking_rooms khong khop room_quantity"));
    }

    // ── e. Trần lượt dùng khuyến mãi ───────────────────────────────────────
    @Test
    @DisplayName("e. used_count vượt usage_limit bị CHECK chặn")
    void usedCountCannotExceedUsageLimit() {
        assertThatThrownBy(() -> insertPromotion("VUOTTRAN", 11, 10))
                .satisfies(thrown -> assertThat(sqlState(thrown)).isEqualTo(CHECK_VIOLATION));
    }

    @Test
    @DisplayName("e2. usage_limit NULL nghĩa là không giới hạn, không bị chặn")
    void nullUsageLimitMeansUnlimited() {
        assertThatCode(() -> insertPromotion("KHONGGIOIHAN", 500, null)).doesNotThrowAnyException();
    }

    // ── Ánh xạ kiểu dữ liệu rủi ro, đi qua ĐÚNG đường JPA ─────────────────
    @Test
    @DisplayName("inet và jsonb đi qua entity JPA đọc/ghi được, không chỉ qua SQL thô")
    void riskyColumnTypesRoundTripThroughJpa() {
        // Hai kiểu này là chỗ ddl-auto=validate KHÔNG bảo vệ được: validate chỉ
        // so kiểu cột, không chứng minh Hibernate đọc/ghi nổi giá trị. Sai ánh
        // xạ ở đây chỉ lộ ra lúc chạy thật.
        Long bookingId = jdbc.queryForObject("SELECT id FROM bookings WHERE id = 1", Long.class);

        tx.executeWithoutResult(status -> {
            Booking booking = bookings.findById(bookingId).orElseThrow();
            // Cập nhật một cột chẳng liên quan gì tới số phòng PHẢI đi lọt, kể
            // cả khi đơn chưa gán phòng nào — trigger chỉ canh room_quantity.
            booking.setClientIp("203.0.113.9");
            booking.setUserAgent("Mozilla/5.0 (test)");
            bookings.save(booking);

            OutboundEmail email = new OutboundEmail();
            email.setBooking(booking);
            email.setTemplate("booking-confirmed");
            email.setToEmail("k1@example.com");
            email.setPayload("{\"code\":\"TVH000001\",\"nights\":2}");
            emails.save(email);
        });

        tx.executeWithoutResult(status -> {
            assertThat(bookings.findById(bookingId).orElseThrow().getClientIp())
                    .as("inet đọc lại qua JPA")
                    .startsWith("203.0.113.9");

            OutboundEmail reloaded = emails.findAll().getFirst();
            assertThat(reloaded.getPayload()).as("jsonb đọc lại qua JPA").contains("TVH000001");
        });

        // Và Postgres thật sự hiểu nội dung đó là JSON, không phải chuỗi.
        assertThat(jdbc.queryForObject(
                "SELECT payload ->> 'code' FROM outbound_emails LIMIT 1", String.class))
                .isEqualTo("TVH000001");
        assertThat(jdbc.queryForObject(
                "SELECT host(client_ip) FROM bookings WHERE id = 1", String.class))
                .isEqualTo("203.0.113.9");
    }

    // ── Tiện ích ───────────────────────────────────────────────────────────
    private void insertBooking(int id, int roomQuantity) {
        jdbc.update("""
                INSERT INTO bookings (id, code, access_token, guest_name, guest_email, guest_phone,
                                      check_in, check_out, adults, room_type_id,
                                      room_type_name_snapshot, unit_price_snapshot, room_quantity,
                                      subtotal_amount, total_amount, status)
                VALUES (?, ?, ?, ?, ?, '0900000000', DATE '2026-10-01', DATE '2026-10-05', 1, 1,
                        'Phòng Vườn', 850000, ?, 3400000, 3400000, 'PENDING_PAYMENT')
                """,
                id, "TVH00000" + id, String.format("%032d", id),
                "Khách " + id, "k" + id + "@example.com", roomQuantity);
    }

    private void assignRoom(int bookingId, String checkIn, String checkOut) {
        assignRoomTo(bookingId, 1, checkIn, checkOut);
    }

    private void assignRoomTo(int bookingId, int roomId, String checkIn, String checkOut) {
        jdbc.update("""
                INSERT INTO booking_rooms (booking_id, room_id, check_in, check_out, status)
                VALUES (?, ?, CAST(? AS date), CAST(? AS date), 'ACTIVE')
                """, bookingId, roomId, checkIn, checkOut);
    }

    private void insertPromotion(String code, int usedCount, Integer usageLimit) {
        jdbc.update("""
                INSERT INTO promotions (code, name, discount_type, discount_value,
                                        starts_at, ends_at, used_count, usage_limit)
                VALUES (?, ?, 'PERCENT', 10, now(), now() + interval '30 day', ?, ?)
                """, code, code, usedCount, usageLimit);
    }

    /**
     * Thông báo gốc từ Postgres.
     *
     * <p>Không so bằng {@code hasMessageContaining} trên ngoại lệ ngoài cùng:
     * lỗi bắn lúc COMMIT được Spring bọc lại thành "Unable to commit against
     * JDBC Connection", và thông báo thật nằm sâu trong chuỗi nguyên nhân.
     */
    private static String rootMessage(Throwable thrown) {
        Throwable current = thrown;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    /** Lần theo chuỗi nguyên nhân để lấy SQLSTATE gốc của Postgres. */
    private static String sqlState(Throwable thrown) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }
}
