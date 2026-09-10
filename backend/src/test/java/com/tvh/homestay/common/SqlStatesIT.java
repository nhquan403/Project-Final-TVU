package com.tvh.homestay.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.tvh.homestay.schema.AbstractPostgresIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Phân biệt "phòng vừa bị người khác lấy mất" với "có bug trong mã".
 *
 * <p>Vòng thử gán phòng chỉ được thử lại khi đụng ràng buộc chống trùng lịch.
 * Nếu nó bắt chung mọi {@code DataIntegrityViolationException}, một lỗi khoá
 * ngoại — tức là bug thật — sẽ bị báo cho khách thành "hết phòng", vòng thử
 * chạy hết năm lượt rồi trả 409, và bug đó không bao giờ được ai phát hiện.
 *
 * <p>Test này dựng lỗi bằng cơ sở dữ liệu THẬT chứ không giả lập ngoại lệ: mã
 * SQLSTATE phải đúng cái PostgreSQL sinh ra, không phải cái ta tưởng nó sinh ra.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SqlStatesIT extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Vi phạm khoá ngoại KHÔNG bị nhận nhầm là vi phạm ràng buộc chống trùng")
    void foreignKeyViolationIsNotAnExclusionViolation() {
        Throwable thrown = catchThrowable(() -> jdbc.update("""
                INSERT INTO booking_rooms (booking_id, room_id, check_in, check_out, status)
                VALUES (999999, 999999, DATE '2027-01-01', DATE '2027-01-03', 'ACTIVE')
                """));

        assertThat(thrown).isNotNull();
        assertThat(SqlStates.sqlStateOf(thrown))
                .as("23503 = vi phạm khoá ngoại")
                .isEqualTo("23503");
        assertThat(SqlStates.isExclusionViolation(thrown))
                .as("bắt nhầm mã này thành 'hết phòng' sẽ che mất một bug thật")
                .isFalse();
    }

    @Test
    @DisplayName("Vi phạm ràng buộc EXCLUDE được nhận đúng là 23P01")
    void exclusionViolationIsRecognised() {
        jdbc.execute("DELETE FROM booking_status_history");
        jdbc.execute("DELETE FROM payments");
        jdbc.execute("DELETE FROM bookings");
        jdbc.execute("DELETE FROM rooms");
        jdbc.execute("DELETE FROM room_types");
        jdbc.update("""
                INSERT INTO room_types (id, code, slug, name, base_price, capacity_adults)
                VALUES (1, 'GARDEN', 'phong-vuon', 'Phòng Vườn', 850000, 2)
                """);
        jdbc.update("INSERT INTO rooms (id, room_type_id, room_number) VALUES (1, 1, '101')");
        for (int id = 1; id <= 2; id++) {
            jdbc.update("""
                    INSERT INTO bookings (id, code, access_token, guest_name, guest_email, guest_phone,
                                          check_in, check_out, adults, room_type_id,
                                          room_type_name_snapshot, unit_price_snapshot, room_quantity,
                                          subtotal_amount, total_amount, status)
                    VALUES (?, ?, ?, 'Khách', 'k@example.com', '0900000000',
                            DATE '2027-01-01', DATE '2027-01-05', 1, 1,
                            'Phòng Vườn', 850000, 1, 3400000, 3400000, 'PENDING_PAYMENT')
                    """, id, "TVH00000" + id, String.format("%032d", id));
        }
        jdbc.update("""
                INSERT INTO booking_rooms (booking_id, room_id, check_in, check_out, status)
                VALUES (1, 1, DATE '2027-01-01', DATE '2027-01-05', 'ACTIVE')
                """);

        Throwable thrown = catchThrowable(() -> jdbc.update("""
                INSERT INTO booking_rooms (booking_id, room_id, check_in, check_out, status)
                VALUES (2, 1, DATE '2027-01-03', DATE '2027-01-07', 'ACTIVE')
                """));

        assertThat(SqlStates.isExclusionViolation(thrown)).isTrue();
    }
}
