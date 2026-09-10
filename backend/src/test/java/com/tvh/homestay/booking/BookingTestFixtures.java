package com.tvh.homestay.booking;

import com.tvh.homestay.booking.dto.BookingDtos.CreateBookingRequest;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dữ liệu nền dùng chung cho các test đặt phòng.
 *
 * <p>Dựng bằng SQL thô chứ không qua JPA: các test này quan tâm hành vi của
 * cơ sở dữ liệu, và dựng nền bằng chính tầng đang được kiểm là tự bịt mắt.
 */
final class BookingTestFixtures {

    static final long ROOM_TYPE_ID = 1L;

    private BookingTestFixtures() {}

    /** Xoá sạch theo đúng thứ tự khoá ngoại, rồi dựng lại một loại phòng với N phòng. */
    static void reset(JdbcTemplate jdbc, int roomCount) {
        jdbc.execute("DELETE FROM booking_status_history");
        jdbc.execute("DELETE FROM outbound_emails");
        jdbc.execute("DELETE FROM payment_webhook_events");
        jdbc.execute("DELETE FROM payments");
        // Xoá bookings kéo theo booking_rooms qua ON DELETE CASCADE. Xoá
        // booking_rooms riêng lẻ sẽ để lại đơn có room_quantity không khớp và
        // chính constraint trigger sẽ chặn bước dọn dẹp.
        jdbc.execute("DELETE FROM bookings");
        jdbc.execute("DELETE FROM rooms");
        jdbc.execute("DELETE FROM room_types");
        jdbc.execute("DELETE FROM promotions");

        jdbc.update("""
                INSERT INTO room_types (id, code, slug, name, base_price,
                                        capacity_adults, capacity_children, bed_info)
                VALUES (?, 'GARDEN', 'phong-vuon', 'Phòng Vườn', 850000, 2, 2, '1 giường đôi')
                """, ROOM_TYPE_ID);
        for (int i = 1; i <= roomCount; i++) {
            jdbc.update(
                    "INSERT INTO rooms (id, room_type_id, room_number, status) VALUES (?, ?, ?, 'AVAILABLE')",
                    i, ROOM_TYPE_ID, "10" + i);
        }
    }

    static CreateBookingRequest request(LocalDate checkIn, LocalDate checkOut, int roomQuantity, int seq) {
        return new CreateBookingRequest(
                ROOM_TYPE_ID,
                checkIn,
                checkOut,
                roomQuantity,
                2 * roomQuantity,
                0,
                "Khách " + seq,
                "khach" + seq + "@example.com",
                "09000000" + String.format("%02d", seq),
                null,
                null);
    }
}
