package com.tvh.homestay.report;

import com.tvh.homestay.report.dto.DashboardSummary.MonthlyValue;
import com.tvh.homestay.report.dto.DashboardSummary.TopRoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tổng hợp số liệu BẰNG SQL, không kéo dữ liệu về tầng Java.
 *
 * <h2>Quy ước múi giờ, và vì sao nó không đồng đều</h2>
 *
 * <p>{@code bookings.check_in} là kiểu {@code date} — một ngày lịch, không có
 * giờ và không có múi giờ, nên {@code date_trunc} trên nó đã đúng sẵn. Ngược
 * lại {@code created_at} và {@code paid_at} là {@code timestamptz}; gom nhóm
 * chúng mà không {@code AT TIME ZONE 'Asia/Ho_Chi_Minh'} sẽ đẩy đơn tạo lúc
 * 00:00–07:00 giờ Việt Nam ngày mùng 1 về tháng trước. Áp máy móc
 * {@code AT TIME ZONE} lên cả cột {@code date} thì lại sai theo chiều ngược
 * lại, nên hai loại cột được xử lý khác nhau — có chủ ý.
 */
@Repository
public class RevenueReportRepository {

    /** Các trạng thái được tính là đơn "đã chốt". */
    private static final String SETTLED_STATUSES = "('CONFIRMED','CHECKED_IN','CHECKED_OUT')";

    private static final String MONTH_SERIES = """
            WITH months AS (
                SELECT m::date AS month_start,
                       (m + interval '1 month')::date AS month_end
                FROM generate_series(
                        date_trunc('month', CAST(:periodStart AS date)),
                        CAST(:periodEnd AS date) - interval '1 day',
                        interval '1 month') AS m
            )
            """;

    private final JdbcClient jdbc;

    public RevenueReportRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Giá trị đơn đã chốt, gom theo tháng NHẬN PHÒNG. */
    public List<MonthlyValue> bookingValueByMonth(LocalDate from, LocalDate to) {
        return jdbc.sql(MONTH_SERIES + """
                SELECT m.month_start AS month,
                       COALESCE((
                           SELECT SUM(b.total_amount) FROM bookings b
                            WHERE b.status IN """ + SETTLED_STATUSES + """
                              AND b.check_in >= m.month_start AND b.check_in < m.month_end
                       ), 0) AS value
                  FROM months m ORDER BY m.month_start
                """)
                .param("periodStart", from)
                .param("periodEnd", to)
                .query((rs, row) -> new MonthlyValue(
                        rs.getObject("month", LocalDate.class), rs.getBigDecimal("value")))
                .list();
    }

    /** Tiền THỰC SỰ đã về tài khoản, gom theo tháng TIỀN VỀ. */
    public List<MonthlyValue> amountReceivedByMonth(LocalDate from, LocalDate to) {
        return jdbc.sql(MONTH_SERIES + """
                SELECT m.month_start AS month,
                       COALESCE((
                           SELECT SUM(p.amount_received) FROM payments p
                            WHERE p.status IN ('SUCCEEDED','OVERPAID')
                              AND p.paid_at IS NOT NULL
                              AND (p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date >= m.month_start
                              AND (p.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date < m.month_end
                       ), 0) AS value
                  FROM months m ORDER BY m.month_start
                """)
                .param("periodStart", from)
                .param("periodEnd", to)
                .query((rs, row) -> new MonthlyValue(
                        rs.getObject("month", LocalDate.class), rs.getBigDecimal("value")))
                .list();
    }

    /**
     * Tỉ lệ lấp đầy theo tháng.
     *
     * <p><b>Mẫu số là TỔNG SỐ PHÒNG VẬT LÝ, không lọc theo {@code status}.</b>
     * Lọc theo {@code status = 'AVAILABLE'} khiến mẫu số phản ánh trạng thái
     * HIỆN TẠI trong khi tử số là dữ liệu LỊCH SỬ: chuyển ba phòng sang bảo trì
     * hôm nay là tỉ lệ lấp đầy của mọi tháng đã qua nhảy vọt, và có thể vượt
     * 100%. Tổng số phòng vật lý ổn định hơn nhiều.
     *
     * <p>{@code LEAST}/{@code GREATEST} cắt phần nằm ngoài tháng, nên một đơn
     * vắt qua ranh giới tháng chỉ được tính đúng số đêm thuộc tháng đó. Kết quả
     * vẫn kẹp {@code LEAST(ratio, 1)} — một lớp chặn thứ hai cho những tình
     * huống dữ liệu chưa nghĩ tới.
     */
    public List<MonthlyValue> occupancyByMonth(LocalDate from, LocalDate to) {
        return jdbc.sql(MONTH_SERIES + """
                , capacity AS (SELECT COUNT(*)::numeric AS rooms FROM rooms)
                SELECT m.month_start AS month,
                       CASE WHEN c.rooms = 0 THEN 0
                            ELSE LEAST(
                                COALESCE((
                                    SELECT SUM(LEAST(br.check_out, m.month_end)
                                             - GREATEST(br.check_in, m.month_start))
                                      FROM booking_rooms br
                                      JOIN bookings b ON b.id = br.booking_id
                                     WHERE br.status = 'ACTIVE'
                                       AND b.status IN """ + SETTLED_STATUSES + """
                                       AND br.stay && daterange(m.month_start, m.month_end, '[)')
                                ), 0)::numeric
                                / ((m.month_end - m.month_start)::numeric * c.rooms),
                                1)
                       END AS value
                  FROM months m CROSS JOIN capacity c ORDER BY m.month_start
                """)
                .param("periodStart", from)
                .param("periodEnd", to)
                .query((rs, row) -> new MonthlyValue(
                        rs.getObject("month", LocalDate.class), rs.getBigDecimal("value")))
                .list();
    }

    /** Số đơn mới, gom theo tháng TẠO ĐƠN (giờ Việt Nam). */
    public List<MonthlyValue> newBookingsByMonth(LocalDate from, LocalDate to) {
        return jdbc.sql(MONTH_SERIES + """
                SELECT m.month_start AS month,
                       COALESCE((
                           SELECT COUNT(*) FROM bookings b
                            WHERE (b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date >= m.month_start
                              AND (b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date < m.month_end
                       ), 0) AS value
                  FROM months m ORDER BY m.month_start
                """)
                .param("periodStart", from)
                .param("periodEnd", to)
                .query((rs, row) -> new MonthlyValue(
                        rs.getObject("month", LocalDate.class), rs.getBigDecimal("value")))
                .list();
    }

    /** Đếm đơn theo trạng thái trong kỳ, trục TẠO ĐƠN. Dùng cho tỉ lệ huỷ. */
    public long countCreated(LocalDate from, LocalDate to, List<String> statuses) {
        String filter = statuses.isEmpty() ? "" : " AND b.status IN (:statuses)";
        var spec = jdbc.sql("""
                SELECT COUNT(*) FROM bookings b
                 WHERE (b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date >= CAST(:periodStart AS date)
                   AND (b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date < CAST(:periodEnd AS date)""" + filter)
                .param("periodStart", from)
                .param("periodEnd", to);
        if (!statuses.isEmpty()) {
            spec = spec.param("statuses", statuses);
        }
        return spec.query(Long.class).single();
    }

    /** Loại phòng bán chạy nhất trong kỳ, trục NHẬN PHÒNG. */
    public List<TopRoomType> topRoomTypes(LocalDate from, LocalDate to, int limit) {
        return jdbc.sql("""
                SELECT b.room_type_name_snapshot AS name,
                       COUNT(*)                  AS bookings,
                       SUM(b.total_amount)       AS value
                  FROM bookings b
                 WHERE b.status IN """ + SETTLED_STATUSES + """
                   AND b.check_in >= CAST(:periodStart AS date)
                   AND b.check_in <  CAST(:periodEnd AS date)
                 GROUP BY b.room_type_name_snapshot
                 ORDER BY bookings DESC, value DESC
                 LIMIT :limit
                """)
                .param("periodStart", from)
                .param("periodEnd", to)
                .param("limit", limit)
                .query((rs, row) -> new TopRoomType(
                        rs.getString("name"), rs.getLong("bookings"), rs.getBigDecimal("value")))
                .list();
    }

    public BigDecimal sum(List<MonthlyValue> values) {
        return values.stream()
                .map(MonthlyValue::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
