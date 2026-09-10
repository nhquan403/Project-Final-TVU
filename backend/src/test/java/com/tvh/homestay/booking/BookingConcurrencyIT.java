package com.tvh.homestay.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.tvh.homestay.booking.exception.BookingExceptions.RoomNotAvailable;
import com.tvh.homestay.schema.AbstractPostgresIT;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Chứng minh hệ thống KHÔNG THỂ bán trùng phòng.
 *
 * <p><b>Vì sao gọi thẳng {@link BookingService} thay vì đi qua HTTP.</b> Giới
 * hạn tần suất của Phase 4 là thật và đúng: 10 request/phút/IP và 10 đơn/giờ
 * cho một số điện thoại. Hai mươi luồng từ cùng một máy sẽ nhận 429 chứ không
 * phải 409, và test sẽ kết luận sai về thứ nó định đo. Nới hạn mức cho test là
 * đi kiểm một cấu hình khác với cấu hình chạy thật, nên không làm.
 *
 * <p>Việc cần đo ở đây nằm trong tầng service và tầng cơ sở dữ liệu — thiết kế
 * transaction và ràng buộc {@code EXCLUDE} — nên bỏ qua chuỗi filter không làm
 * mất điều gì. Việc ánh xạ ngoại lệ sang HTTP 409 được
 * {@code BookingLifecycleIT} kiểm riêng.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BookingConcurrencyIT extends AbstractPostgresIT {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 10, 1);
    private static final LocalDate CHECK_OUT = LocalDate.of(2026, 10, 4);

    @Autowired
    private BookingService bookings;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanSlate() {
        BookingTestFixtures.reset(jdbc, 1);
    }

    /** Kết quả của một luồng: thành công, hết phòng, hay lỗi ngoài dự kiến. */
    private record Outcome(boolean created, boolean roomNotAvailable, Throwable unexpected) {
        boolean unexpectedPresent() {
            return unexpected != null;
        }
    }

    private List<Outcome> runConcurrently(int threads, List<Callable<Void>> ignored, int roomQuantity) {
        CyclicBarrier startLine = new CyclicBarrier(threads);
        AtomicInteger seq = new AtomicInteger();
        List<Outcome> outcomes = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    // Chờ đủ mặt rồi cùng lao vào: không có hàng rào này thì
                    // các luồng chạy nối đuôi nhau và không có va chạm nào.
                    startLine.await();
                    try {
                        bookings.create(
                                BookingTestFixtures.request(
                                        CHECK_IN, CHECK_OUT, roomQuantity, seq.incrementAndGet()),
                                null, "127.0.0.1", "junit");
                        return new Outcome(true, false, null);
                    } catch (RoomNotAvailable e) {
                        return new Outcome(false, true, null);
                    } catch (Throwable t) {
                        return new Outcome(false, false, t);
                    }
                }));
            }
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return outcomes;
    }

    private int activeBookingRoomCount() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM booking_rooms WHERE status = 'ACTIVE'", Integer.class);
    }

    @Test
    @DisplayName("lastRoom — 20 luồng tranh 1 phòng: đúng 1 thành công, 19 báo hết phòng, không 500 nào")
    void lastRoom() {
        BookingTestFixtures.reset(jdbc, 1);

        List<Outcome> outcomes = runConcurrently(20, List.of(), 1);

        assertThat(outcomes.stream().filter(Outcome::unexpectedPresent).toList())
                .as("không được có lỗi ngoài dự kiến — mỗi cái là một HTTP 500 ngoài đời")
                .isEmpty();
        assertThat(outcomes.stream().filter(Outcome::created).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(Outcome::roomNotAvailable).count()).isEqualTo(19);
        assertThat(activeBookingRoomCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("multiRoom — 3 luồng, 3 phòng: CẢ BA thành công (test bắt lỗi thiết kế transaction)")
    void multiRoom() {
        // Đây là test quan trọng nhất của cả phase. Với loại phòng chỉ có MỘT
        // phòng, vòng thử thoát ngay ở thất bại đầu tiên nên một thiết kế
        // transaction sai vẫn cho ra kết quả đúng. Bug chỉ lộ khi loại phòng có
        // từ hai phòng trở lên — tức là toàn bộ dữ liệu thật.
        BookingTestFixtures.reset(jdbc, 3);

        List<Outcome> outcomes = runConcurrently(3, List.of(), 1);

        assertThat(outcomes.stream().filter(Outcome::unexpectedPresent).toList())
                .as("gộp vòng thử vào một transaction sẽ hỏng ở đây với 25P02 "
                        + "hoặc UnexpectedRollbackException")
                .isEmpty();
        assertThat(outcomes.stream().filter(Outcome::created).count()).isEqualTo(3);
        assertThat(activeBookingRoomCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("partialAllocation — không bao giờ tồn tại đơn có số phòng gán lệch room_quantity")
    void partialAllocation() {
        BookingTestFixtures.reset(jdbc, 3);

        CyclicBarrier startLine = new CyclicBarrier(2);
        List<Outcome> outcomes = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Outcome>> futures = new ArrayList<>();
            // Một bên xin 3 phòng, một bên xin 1: hai bên chắc chắn giẫm lên nhau.
            for (int roomQuantity : new int[] {3, 1}) {
                final int quantity = roomQuantity;
                futures.add(pool.submit(() -> {
                    startLine.await();
                    try {
                        bookings.create(
                                BookingTestFixtures.request(CHECK_IN, CHECK_OUT, quantity, quantity),
                                null, "127.0.0.1", "junit");
                        return new Outcome(true, false, null);
                    } catch (RoomNotAvailable e) {
                        return new Outcome(false, true, null);
                    } catch (Throwable t) {
                        return new Outcome(false, false, t);
                    }
                }));
            }
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        assertThat(outcomes.stream().filter(Outcome::unexpectedPresent).toList()).isEmpty();

        Integer mismatched = jdbc.queryForObject("""
                SELECT count(*) FROM bookings b
                 WHERE b.status NOT IN ('CANCELLED','EXPIRED','NO_SHOW')
                   AND b.room_quantity <> (
                       SELECT count(*) FROM booking_rooms br
                        WHERE br.booking_id = b.id AND br.status = 'ACTIVE')
                """, Integer.class);
        assertThat(mismatched)
                .as("đơn đã thu tiền N phòng mà chỉ giữ được ít hơn là bán thừa phòng cho khách khác")
                .isZero();
    }
}
