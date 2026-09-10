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
