package com.tvh.homestay.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.availability.AvailabilityService;
import com.tvh.homestay.availability.dto.AvailabilityDtos.AvailabilitySearchResponse;
import com.tvh.homestay.availability.dto.AvailabilityDtos.RoomTypeAvailability;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Truy vấn phòng trống đếm đúng trong những tình huống dễ đếm sai nhất. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AvailabilityQueryIT extends AbstractPostgresIT {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 10, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 10, 4);

    @Autowired
    private AvailabilityService availability;

    @Autowired
    private BookingService bookings;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanSlate() {
        BookingTestFixtures.reset(jdbc, 3);
    }

    private RoomTypeAvailability search() {
        AvailabilitySearchResponse response =
                availability.search(CHECK_IN, CHECK_OUT, 2, 0, 1);
        return response.roomTypes().isEmpty() ? null : response.roomTypes().get(0);
    }

    @Test
    @DisplayName("maintenanceRoom — phòng bảo trì ĐANG CÓ booking không bị trừ hai lần")
    void maintenanceRoom() {
        // Phòng 1 có đơn thật; sau đó admin chuyển chính phòng đó sang bảo trì.
        // Cách đếm "tổng phòng khả dụng trừ mọi booking_rooms ACTIVE của loại"
        // sẽ trừ phòng này hai lần: một lần vì nó không còn AVAILABLE, một lần
        // nữa vì nó vẫn có dòng booking ACTIVE. Kết quả: báo còn 1 thay vì 2,
        // và doanh thu mất đi âm thầm.
        bookings.create(BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, 1),
                null, "127.0.0.1", "junit");
        jdbc.update("UPDATE rooms SET status = 'MAINTENANCE' WHERE id = 1");

        assertThat(search().availableCount())
                .as("còn phòng 2 và phòng 3 rảnh")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Loại phòng hết sạch không xuất hiện trong kết quả tìm kiếm")
    void soldOutRoomTypeDisappears() {
        for (int i = 1; i <= 3; i++) {
            bookings.create(BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, i),
                    null, "127.0.0.1", "junit");
        }
        assertThat(availability.search(CHECK_IN, CHECK_OUT, 2, 0, 1).roomTypes()).isEmpty();
    }

    @Test
    @DisplayName("Khoảng nửa mở: trả phòng 10-04 và nhận phòng 10-04 không đụng nhau")
    void adjacentStaysShareTheSameRoom() {
        BookingTestFixtures.reset(jdbc, 1);

        bookings.create(BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, 1),
                null, "127.0.0.1", "junit");
        // Khách B nhận phòng đúng ngày khách A trả. Quy ước [) làm điều này
        // hợp lệ mà không cần một dòng mã nào ở tầng ứng dụng.
        bookings.create(BookingTestFixtures.request(CHECK_OUT, CHECK_OUT.plusDays(3), 1, 2),
                null, "127.0.0.1", "junit");

        Integer sameRoom = jdbc.queryForObject(
                "SELECT count(DISTINCT room_id) FROM booking_rooms WHERE status = 'ACTIVE'",
                Integer.class);
        assertThat(sameRoom).as("cả hai đơn phải nằm trên cùng một phòng vật lý").isEqualTo(1);
    }

    @Test
    @DisplayName("Sức chứa so theo TỪNG phòng, không so tổng khách với một phòng")
    void capacityIsPerRoomNotPerBooking() {
        // Loại phòng chứa 2 người lớn. Bốn khách trong hai phòng là hợp lệ;
        // so tổng 4 khách với sức chứa 2 sẽ từ chối nhầm đơn này.
        assertThat(availability.search(CHECK_IN, CHECK_OUT, 4, 0, 2).roomTypes())
                .as("4 khách / 2 phòng = 2 khách mỗi phòng, vừa đúng sức chứa")
                .isNotEmpty();
        assertThat(availability.search(CHECK_IN, CHECK_OUT, 4, 0, 1).roomTypes())
                .as("4 khách trong 1 phòng thì vượt sức chứa")
                .isEmpty();
    }

    @Test
    @DisplayName("Lịch giá trả ĐỦ mọi ngày, kể cả ngày đã hết phòng")
    void calendarIncludesSoldOutDays() {
        BookingTestFixtures.reset(jdbc, 1);
        bookings.create(BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, 1),
                null, "127.0.0.1", "junit");

        var days = availability
                .dayCalendar(BookingTestFixtures.ROOM_TYPE_ID, CHECK_IN.minusDays(1), CHECK_OUT.plusDays(1))
                .days();

        // Lịch ở frontend coi ngày VẮNG MẶT trong bản đồ là ngày không bị chặn.
        // Bỏ sót một ngày hết phòng nghĩa là cho khách chọn đúng ngày không đặt được.
        assertThat(days).hasSize(5);
        assertThat(days.get("2026-10-01").availableCount()).isZero();
        assertThat(days.get("2026-10-03").availableCount()).isZero();
        assertThat(days.get("2026-09-30").availableCount()).isEqualTo(1);
        // Ngày trả phòng không cần còn phòng — đêm 10-04 vẫn trống.
        assertThat(days.get("2026-10-04").availableCount()).isEqualTo(1);
    }
}
