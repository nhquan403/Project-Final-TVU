package com.tvh.homestay.availability;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn phòng trống.
 *
 * <p>Viết bằng SQL thuần chứ không qua JPA: câu này dùng {@code CROSS JOIN
 * LATERAL}, kiểu {@code daterange} và toán tử {@code &&} — đều là thứ JPQL
 * không có.
 */
@Repository
public class AvailabilityRepository {

    /**
     * Đếm TRỰC TIẾP số phòng vừa khả dụng vừa rảnh, thay vì lấy tổng trừ đi số
     * đã đặt.
     *
     * <p>Cách "tổng trừ đã đặt" sai ở chỗ nó trừ mọi dòng {@code booking_rooms}
     * ACTIVE của loại phòng — kể cả booking nằm trên phòng đã chuyển sang
     * {@code MAINTENANCE}. Phòng bảo trì có booking cũ bị trừ HAI LẦN, website
     * báo thiếu phòng và mất doanh thu âm thầm. Admin được phép chuyển phòng
     * đang có booking sang bảo trì, nên đây là tình huống thiết kế chứ không
     * phải trường hợp hiếm.
     *
     * <p>Toán tử {@code &&} chạy trên chính index GiST mà ràng buộc
     * {@code EXCLUDE} đã tạo — không cần index thêm.
     */
    private static final String FREE_ROOM_COUNT_BY_TYPE = """
            SELECT rt.id                AS room_type_id,
                   rt.code              AS code,
                   rt.slug              AS slug,
                   rt.name              AS name,
                   rt.short_description AS short_description,
                   rt.bed_info          AS bed_info,
                   rt.capacity_adults   AS capacity_adults,
                   rt.capacity_children AS capacity_children,
                   rt.base_price        AS base_price,
                   c.available_count    AS available_count
            FROM room_types rt
            CROSS JOIN LATERAL (
                SELECT count(*) AS available_count
                FROM rooms r
                WHERE r.room_type_id = rt.id
                  AND r.status = 'AVAILABLE'
                  AND NOT EXISTS (
                      SELECT 1 FROM booking_rooms br
                      WHERE br.room_id = r.id
                        AND br.status = 'ACTIVE'
                        AND br.stay && daterange(:checkIn, :checkOut, '[)')
                  )
            ) c
            WHERE rt.active
              AND rt.capacity_adults   >= :adultsPerRoom
              AND rt.capacity_children >= :childrenPerRoom
              AND c.available_count    >= :roomQuantity
            ORDER BY rt.display_order, rt.id
            """;

    /** Id của các phòng vật lý còn rảnh, để tầng gán phòng chọn ứng viên. */
    private static final String FREE_ROOM_IDS = """
            SELECT r.id
            FROM rooms r
            WHERE r.room_type_id = :roomTypeId
              AND r.status = 'AVAILABLE'
              AND NOT EXISTS (
                  SELECT 1 FROM booking_rooms br
                  WHERE br.room_id = r.id
                    AND br.status = 'ACTIVE'
                    AND br.stay && daterange(:checkIn, :checkOut, '[)')
              )
            ORDER BY r.id
            """;

    private final JdbcClient jdbc;

    public AvailabilityRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Một dòng thô từ truy vấn; tầng service ghép thêm giá và ảnh. */
    public record FreeRoomTypeRow(
            Long roomTypeId,
            String code,
            String slug,
            String name,
            String shortDescription,
            String bedInfo,
            int capacityAdults,
            int capacityChildren,
            java.math.BigDecimal basePrice,
            int availableCount) {}

    public List<FreeRoomTypeRow> findFreeRoomTypes(
            LocalDate checkIn,
            LocalDate checkOut,
            int adultsPerRoom,
            int childrenPerRoom,
            int roomQuantity) {
        return jdbc.sql(FREE_ROOM_COUNT_BY_TYPE)
                .param("checkIn", checkIn)
                .param("checkOut", checkOut)
                .param("adultsPerRoom", adultsPerRoom)
                .param("childrenPerRoom", childrenPerRoom)
                .param("roomQuantity", roomQuantity)
                .query(FreeRoomTypeRow.class)
                .list();
    }

    public List<Long> findFreeRoomIds(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return jdbc.sql(FREE_ROOM_IDS)
                .param("roomTypeId", roomTypeId)
                .param("checkIn", checkIn)
                .param("checkOut", checkOut)
                .query(Long.class)
                .list();
    }

    /**
     * Số phòng rảnh của TOÀN HOMESTAY cho từng đêm trong khoảng, trong MỘT truy vấn.
     *
     * <p>Thanh tìm phòng ở trang chủ chạy TRƯỚC khi khách chọn loại phòng, nên
     * nó không có {@code roomTypeId} để hỏi. Không có truy vấn này, lịch trên
     * trang chủ chỉ chặn được ngày quá khứ — còn ngày đã kín phòng vẫn bấm chọn
     * được, và khách chỉ biết mình chọn sai sau khi bấm tìm.
     *
     * <p>Gộp cả khoảng vào một câu lệnh thay vì lặp từng đêm: khoảng tối đa là
     * 120 ngày, và 120 lượt đi về cơ sở dữ liệu cho một lần mở lịch là đủ để
     * người dùng thấy lịch giật.
     *
     * <p>Giá trả về là giá THẤP NHẤT trong các loại phòng còn chỗ đêm đó — giao
     * diện hiển thị dạng "từ X", vì đây là mức khởi điểm chứ không phải giá của
     * một loại phòng cụ thể.
     */
    public java.util.List<NightAvailability> countFreeRoomsPerNight(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                        WITH sellable AS (
                            SELECT r.id, rt.base_price
                              FROM rooms r
                              JOIN room_types rt ON rt.id = r.room_type_id
                             WHERE r.status = 'AVAILABLE' AND rt.active = true
                        ), nights AS (
                            SELECT d::date AS night
                              FROM generate_series(
                                      CAST(:from AS date),
                                      CAST(:to AS date) - 1,
                                      interval '1 day') AS d
                        )
                        SELECT n.night                AS night,
                               count(s.id)            AS free_rooms,
                               min(s.base_price)      AS price
                          FROM nights n
                          LEFT JOIN sellable s ON NOT EXISTS (
                                   SELECT 1 FROM booking_rooms br
                                    WHERE br.room_id = s.id
                                      AND br.status = 'ACTIVE'
                                      AND br.stay && daterange(n.night, n.night + 1, '[)'))
                         GROUP BY n.night
                         ORDER BY n.night
                        """)
                .param("from", from)
                .param("to", to)
                .query((rs, row) -> new NightAvailability(
                        rs.getObject("night", LocalDate.class),
                        rs.getInt("free_rooms"),
                        rs.getBigDecimal("price")))
                .list();
    }

    /** Một đêm của lịch toàn homestay. {@code price} null khi đêm đó không còn phòng nào. */
    public record NightAvailability(
            LocalDate night, int freeRooms, java.math.BigDecimal price) {}

    /** Số phòng rảnh của một loại phòng trong đúng MỘT đêm. Dùng dựng lịch giá. */
    public int countFreeRoomsForNight(Long roomTypeId, LocalDate night) {
        Integer count = jdbc.sql("""
                        SELECT count(*)
                        FROM rooms r
                        WHERE r.room_type_id = :roomTypeId
                          AND r.status = 'AVAILABLE'
                          AND NOT EXISTS (
                              SELECT 1 FROM booking_rooms br
                              WHERE br.room_id = r.id
                                AND br.status = 'ACTIVE'
                                AND br.stay && daterange(:night, :nextDay, '[)')
                          )
                        """)
                .param("roomTypeId", roomTypeId)
                .param("night", night)
                .param("nextDay", night.plusDays(1))
                .query(Integer.class)
                .single();
        return count == null ? 0 : count;
    }
}
