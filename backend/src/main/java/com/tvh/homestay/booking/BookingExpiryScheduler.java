package com.tvh.homestay.booking;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hết hạn giữ chỗ cho các đơn chưa thanh toán.
 *
 * <p><b>Ba điều kiện bảo vệ, không phải một.</b> Cửa sổ đua là có thật: ngân
 * hàng → nhà cung cấp → API thường trễ 5–30 giây, và đồng hồ đếm ngược trên
 * màn hình QR khuyến khích khách bấm chuyển khoản vào đúng phút cuối. Chỉ kiểm
 * "quá hạn" thì kịch bản khách chuyển tiền phút 14 và webhook về phút 16 kết
 * thúc bằng: tiền đã vào tài khoản, phòng đã bán cho người khác, và không bản
 * ghi nào cho thấy chuyện gì đã xảy ra.
 *
 * <p>Trạng thái {@code AWAITING_REVIEW} cố ý KHÔNG nằm trong phạm vi quét: đơn
 * đã có tiền là việc của người, không phải của bộ đếm giờ.
 */
@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    /**
     * Chuyển đơn quá hạn sang EXPIRED và ghi nhật ký trong cùng một câu lệnh.
     *
     * <p>Dùng {@code UPDATE ... RETURNING} + {@code INSERT ... SELECT} vì bulk
     * update đi vòng qua Hibernate: không có mệnh đề RETURNING thì phải truy vấn
     * lại để biết đơn nào vừa đổi, và giữa hai lượt đó trạng thái có thể đã khác.
     */
    private static final String EXPIRE_AND_LOG = """
            WITH expired AS (
                UPDATE bookings b SET status = 'EXPIRED', updated_at = now()
                WHERE b.status = 'PENDING_PAYMENT'
                  AND b.hold_expires_at < :cutoff
                  AND NOT EXISTS (
                      SELECT 1 FROM payments p
                      WHERE p.booking_id = b.id AND p.amount_received > 0)
                  AND NOT EXISTS (
                      SELECT 1 FROM payment_webhook_events e
                      JOIN payments p2 ON p2.id = e.payment_id
                      WHERE p2.booking_id = b.id AND e.processed_at IS NULL)
                RETURNING b.id
            ), logged AS (
                INSERT INTO booking_status_history
                       (booking_id, from_status, to_status, actor, note, created_at)
                SELECT id, 'PENDING_PAYMENT', 'EXPIRED', 'SYSTEM', 'Hết hạn giữ chỗ', now()
                FROM expired
                RETURNING booking_id
            )
            SELECT booking_id FROM logged
            """;

    /** Nhả phòng của đúng tập đơn vừa hết hạn. */
    private static final String RELEASE_ROOMS = """
            UPDATE booking_rooms SET status = 'RELEASED'
             WHERE booking_id = ANY(:bookingIds) AND status = 'ACTIVE'
            """;

    /** Hoàn lượt khuyến mãi cho đúng tập đơn vừa hết hạn. */
    private static final String RELEASE_PROMOTIONS = """
            UPDATE promotions p SET used_count = p.used_count - 1
             WHERE p.used_count > 0
               AND p.id IN (SELECT b.promotion_id FROM bookings b
                             WHERE b.id = ANY(:bookingIds) AND b.promotion_id IS NOT NULL)
            """;

    private final JdbcClient jdbc;
    private final EntityManager entityManager;
    private final Clock clock;
    private final long graceMinutes;

    public BookingExpiryScheduler(
            JdbcClient jdbc,
            EntityManager entityManager,
            Clock clock,
            @Value("${booking.expiry-grace-minutes:5}") long graceMinutes) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
        this.clock = clock;
        this.graceMinutes = graceMinutes;
    }

    @Scheduled(fixedDelayString = "${booking.expiry-scan-ms:60000}")
    public void expireHolds() {
        int expired = runOnce();
        if (expired > 0) {
            log.info("Đã cho hết hạn {} đơn giữ chỗ quá hạn", expired);
        }
    }

    /** Tách khỏi phương thức {@code @Scheduled} để test gọi thẳng được. */
    @Transactional
    public int runOnce() {
        // Mốc cắt tính ở Java từ bean Clock của ứng dụng, không dùng
        // make_interval trong SQL: tham số bind không mang kiểu nên PostgreSQL
        // không giải được nạp chồng của hàm đó và câu lệnh hỏng ngay ở tầng cú
        // pháp. Tính ở đây còn dùng chung một nguồn thời gian với phần còn lại
        // của hệ thống, và test thay đồng hồ được.
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusMinutes(graceMinutes);
        List<Long> expiredIds =
                jdbc.sql(EXPIRE_AND_LOG).param("cutoff", cutoff).query(Long.class).list();
        if (expiredIds.isEmpty()) {
            return 0;
        }
        Long[] ids = expiredIds.toArray(Long[]::new);
        jdbc.sql(RELEASE_PROMOTIONS).param("bookingIds", ids).update();
        jdbc.sql(RELEASE_ROOMS).param("bookingIds", ids).update();

        // Bulk update đi vòng qua Hibernate, nên first-level cache vẫn giữ
        // trạng thái cũ. Không clear thì lượt đọc kế tiếp trong cùng phiên trả
        // về PENDING_PAYMENT cho đơn vừa EXPIRED.
        entityManager.clear();
        return expiredIds.size();
    }
}
