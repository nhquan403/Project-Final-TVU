package com.tvh.homestay.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tvh.homestay.admin.AdminRoomService;
import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureResult;
import com.tvh.homestay.admin.exception.AdminExceptions.ClosureOverlap;
import com.tvh.homestay.availability.AvailabilityService;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Khoảng đóng phòng trừ đúng phòng, đúng đêm, và không bao giờ bị bỏ sót ở
 * đường GÁN PHÒNG.
 *
 * <p>Điều đáng canh nhất không phải con số trên lịch mà là truy vấn chọn phòng
 * vật lý lúc tạo đơn: sót nó thì đơn vẫn tạo được và phòng đang sửa chữa vẫn bị
 * gán — một lỗi im lặng chỉ lộ ra khi khách tới nhận phòng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RoomClosureIT extends AbstractPostgresIT {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 10, 20);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 10, 25);

    @Autowired
    private AvailabilityService availability;

    @Autowired
    private AdminRoomService adminRooms;

    @Autowired
    private BookingService bookings;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanSlate() {
        jdbc.execute("DELETE FROM room_closures");
        BookingTestFixtures.reset(jdbc, 3);
    }

    private RoomClosureResult close(long roomId, LocalDate from, LocalDate to) {
        return adminRooms.addClosure(roomId, new RoomClosureRequest(from, to, "Sơn lại phòng"), null);
    }

    /** Số phòng còn bán được của loại phòng nền, cho đúng khoảng ngày truyền vào. */
    private int freeCount(LocalDate checkIn, LocalDate checkOut) {
        var types = availability.search(checkIn, checkOut, 2, 0, 1).roomTypes();
        return types.isEmpty() ? 0 : types.get(0).availableCount();
    }

    @Test
    @DisplayName("Đóng một phòng làm kho bán GIẢM ĐÚNG 1 trong khoảng, và không đổi ngoài khoảng")
    void closedRangeRemovesRoomFromSearch() {
        assertThat(freeCount(CHECK_IN, CHECK_OUT)).isEqualTo(3);

        close(1L, CHECK_IN, CHECK_OUT);

        assertThat(freeCount(CHECK_IN, CHECK_OUT))
                .as("chỉ phòng 1 bị đóng, hai phòng còn lại vẫn bán được")
                .isEqualTo(2);
        assertThat(freeCount(CHECK_OUT, CHECK_OUT.plusDays(3)))
                .as("khoảng ngày nằm hoàn toàn sau khoảng đóng thì không bị ảnh hưởng")
                .isEqualTo(3);
        assertThat(freeCount(CHECK_IN.minusDays(5), CHECK_IN))
                .as("khoảng ngày nằm hoàn toàn trước khoảng đóng cũng vậy")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("Phòng đang đóng KHÔNG BAO GIỜ được gán cho đơn mới")
    void closedRoomIsNeverAssignedToNewBooking() {
        // Bài kiểm quan trọng nhất của lớp này. Ba truy vấn kia chỉ chi phối thứ
        // khách NHÌN THẤY; sót truy vấn chọn phòng thì đơn được tạo thật trên
        // một phòng đang sửa chữa và không màn hình nào báo gì cả.
        close(1L, CHECK_IN, CHECK_OUT);
        close(2L, CHECK_IN, CHECK_OUT);

        bookings.create(BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, 1),
                null, "127.0.0.1", "junit");

        List<Long> assigned = jdbc.queryForList(
                "SELECT room_id FROM booking_rooms WHERE status = 'ACTIVE'", Long.class);
        assertThat(assigned)
                .as("chỉ còn phòng 3 là hợp lệ")
                .containsExactly(3L);
    }

    @Test
    @DisplayName("Quy ước nửa mở: đêm to_date-1 bị chặn, đêm to_date thì không")
    void halfOpenBoundary() {
        BookingTestFixtures.reset(jdbc, 1);
        close(1L, CHECK_IN, CHECK_OUT);

        assertThat(freeCount(CHECK_OUT.minusDays(1), CHECK_OUT))
                .as("đêm 24/10 là đêm cuối nằm trong khoảng đóng")
                .isZero();
        assertThat(freeCount(CHECK_OUT, CHECK_OUT.plusDays(1)))
                .as("đêm 25/10 là đêm mở bán lại — chặn nó là lệch một ngày")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Lịch trừ đúng đêm bị đóng và vẫn trả ĐỦ mọi ngày")
    void calendarShowsClosedNights() {
        BookingTestFixtures.reset(jdbc, 1);
        close(1L, CHECK_IN, CHECK_IN.plusDays(2));

        // Lịch một loại phòng.
        var byType = availability
                .dayCalendar(BookingTestFixtures.ROOM_TYPE_ID, CHECK_IN.minusDays(1), CHECK_IN.plusDays(3))
                .days();
        assertThat(byType).as("bốn đêm, không đêm nào rơi mất").hasSize(4);
        assertThat(byType.get(CHECK_IN.minusDays(1).toString()).availableCount()).isEqualTo(1);
        assertThat(byType.get(CHECK_IN.toString()).availableCount()).isZero();
        assertThat(byType.get(CHECK_IN.plusDays(1).toString()).availableCount()).isZero();
        assertThat(byType.get(CHECK_IN.plusDays(2).toString()).availableCount()).isEqualTo(1);

        // Lịch toàn homestay đi qua một truy vấn KHÁC, có LEFT JOIN. Điều kiện
        // khoảng đóng đặt nhầm xuống WHERE sẽ làm những đêm 0 phòng biến mất
        // khỏi bản đồ — và frontend coi ngày vắng mặt là ngày KHÔNG bị chặn.
        var all = availability
                .dayCalendar(null, CHECK_IN.minusDays(1), CHECK_IN.plusDays(3))
                .days();
        assertThat(all).as("đêm hết sạch phòng phải có mặt với số 0, không được rơi mất").hasSize(4);
        assertThat(all.get(CHECK_IN.toString()).availableCount()).isZero();
        assertThat(all.get(CHECK_IN.plusDays(2).toString()).availableCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Hai khoảng đóng chồng nhau bị từ chối bằng lỗi có mã, không phải 500")
    void overlappingClosuresRejected() {
        close(1L, CHECK_IN, CHECK_OUT);

        assertThatThrownBy(() -> close(1L, CHECK_OUT.minusDays(1), CHECK_OUT.plusDays(3)))
                .isInstanceOf(ClosureOverlap.class)
                .extracting(thrown -> ((ClosureOverlap) thrown).getCode())
                .isEqualTo("CLOSURE_OVERLAP");

        // Khoảng liền kề KHÔNG chồng: 25/10 là ngày mở bán lại của khoảng đầu.
        assertThat(close(1L, CHECK_OUT, CHECK_OUT.plusDays(3)).closure().nights()).isEqualTo(3);
        // Cùng khoảng ngày trên phòng KHÁC cũng không chồng.
        assertThat(close(2L, CHECK_IN, CHECK_OUT).closure().nights()).isEqualTo(5);
    }

    @Test
    @DisplayName("Đóng phòng đang có đơn KHÔNG huỷ đơn — chỉ liệt kê ra")
    void closingRoomWithBookingDoesNotCancelIt() {
        BookingTestFixtures.reset(jdbc, 1);
        var booking = bookings.create(BookingTestFixtures.request(CHECK_IN, CHECK_OUT, 1, 1),
                null, "127.0.0.1", "junit");

        RoomClosureResult result = close(1L, CHECK_IN, CHECK_OUT);

        assertThat(result.affectedBookings())
                .extracting(affected -> affected.code())
                .containsExactly(booking.code());
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM bookings WHERE code = ?", String.class, booking.code()))
                .as("đơn vẫn nguyên trạng thái — hệ thống không tự quyết thay người dùng")
                .isEqualTo("PENDING_PAYMENT");
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM booking_rooms WHERE status = 'ACTIVE'", Integer.class))
                .as("phòng vẫn được giữ cho khách")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Đơn không giao với khoảng đóng thì không bị liệt kê")
    void unrelatedBookingIsNotReported() {
        BookingTestFixtures.reset(jdbc, 1);
        bookings.create(BookingTestFixtures.request(CHECK_OUT, CHECK_OUT.plusDays(3), 1, 1),
                null, "127.0.0.1", "junit");

        assertThat(close(1L, CHECK_IN, CHECK_OUT).affectedBookings())
                .as("đơn bắt đầu đúng ngày mở bán lại thì không liên quan gì tới khoảng đóng")
                .isEmpty();
    }

    @Test
    @DisplayName("Khoảng đã qua biến khỏi danh sách quản trị nhưng vẫn còn trong bảng")
    void expiredClosuresLeaveTheAdminList() {
        LocalDate today = LocalDate.now();
        close(1L, today.plusDays(10), today.plusDays(12));
        // Khoảng đã kết thúc: không còn chặn đêm nào từ hôm nay trở đi.
        close(1L, today.minusDays(5), today.minusDays(2));
        // Khoảng đang diễn ra dở phải CÒN hiện ra — sửa xong sớm thì phải xoá được.
        close(1L, today.minusDays(1), today.plusDays(1));

        assertThat(adminRooms.listClosures(1L))
                .as("chỉ khoảng còn chặn đêm từ hôm nay trở đi mới thao tác được")
                .hasSize(2)
                .allMatch(view -> view.toDate().isAfter(today));

        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM room_closures WHERE room_id = 1", Integer.class))
                .as("lọc ở màn hình, KHÔNG xoá dữ liệu — bản ghi cũ là vết lịch sử")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("Xoá khoảng đóng trả phòng về kho bán ngay")
    void deletingClosureReopensRoom() {
        var result = close(1L, CHECK_IN, CHECK_OUT);
        assertThat(freeCount(CHECK_IN, CHECK_OUT)).isEqualTo(2);

        adminRooms.removeClosure(result.closure().id());

        assertThat(freeCount(CHECK_IN, CHECK_OUT))
                .as("xoá xong là bán lại được, không cần thao tác thứ hai")
                .isEqualTo(3);
    }
}
