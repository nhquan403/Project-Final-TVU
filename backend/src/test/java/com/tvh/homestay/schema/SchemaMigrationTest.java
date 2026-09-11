package com.tvh.homestay.schema;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Chứng minh hai điều cùng lúc:
 *
 * <ol>
 *   <li>Sáu file migration chạy hết trên PostgreSQL 16 thật.
 *   <li>Spring context khởi động được với {@code ddl-auto=validate} — nghĩa là
 *       19 entity khớp từng cột với schema mà migration dựng ra.
 * </ol>
 *
 * <p>Điều thứ hai là điều đắt giá: {@code validate} so từng cột, từng kiểu.
 * Test này chạy được nghĩa là không có cái lệch nào còn sót lại để phát hiện
 * vào lúc chạy thật.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SchemaMigrationTest extends AbstractPostgresIT {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Migration dựng đúng 20 bảng")
    void migrationsCreateExpectedTables() throws Exception {
        assertThat(queryForInt("""
                SELECT count(*) FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_name <> 'flyway_schema_history'
                """)).isEqualTo(20);
    }

    @Test
    @DisplayName("Mọi migration đều Success, không cái nào Failed")
    void everyMigrationSucceeded() throws Exception {
        // V7 (booking_notes) là migration thứ bảy, thêm ở Phase 7.
        assertThat(queryForInt("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo(7);
        assertThat(queryForInt("SELECT count(*) FROM flyway_schema_history WHERE NOT success")).isZero();
    }

    @Test
    @DisplayName("btree_gist đã cài — điều kiện sống còn của ràng buộc EXCLUDE")
    void btreeGistInstalled() throws Exception {
        assertThat(queryForInt("SELECT count(*) FROM pg_extension WHERE extname = 'btree_gist'")).isEqualTo(1);
    }

    @Test
    @DisplayName("Ràng buộc EXCLUDE tồn tại trên booking_rooms")
    void exclusionConstraintExists() throws Exception {
        assertThat(queryForInt("""
                SELECT count(*) FROM pg_constraint
                 WHERE conname = 'booking_rooms_no_overlap' AND contype = 'x'
                """)).isEqualTo(1);
    }

    @Test
    @DisplayName("stay là cột sinh tự động, không phải cột thường")
    void stayIsGenerated() throws Exception {
        assertThat(queryForInt("""
                SELECT count(*) FROM information_schema.columns
                 WHERE table_name = 'booking_rooms' AND column_name = 'stay'
                   AND is_generated = 'ALWAYS'
                """)).isEqualTo(1);
    }

    @Test
    @DisplayName("Mọi cột enum trong hợp đồng đều có CHECK")
    void everyEnumColumnHasCheckConstraint() throws Exception {
        // Tên ràng buộc, không phải tên cột: một cột có thể mang nhiều CHECK,
        // và cái cần canh là CHECK LIỆT KÊ GIÁ TRỊ có tồn tại hay không.
        String[] required = {
            "ck_users_role",
            "ck_amenities_category",
            "ck_rooms_status",
            "ck_bookings_status",
            "ck_bookings_pay_status",
            "ck_booking_rooms_status",
            "ck_bsh_actor",
            "ck_bsh_from_status",
            "ck_bsh_to_status",
            "ck_payments_provider",
            "ck_payments_status",
            "ck_payments_reconcile",
            "ck_pwe_result",
            "ck_outbound_emails_status",
            "ck_promotions_type",
            "ck_reviews_status",
        };
        for (String name : required) {
            assertThat(queryForInt(
                    "SELECT count(*) FROM pg_constraint WHERE contype = 'c' AND conname = '" + name + "'"))
                    .as("thiếu CHECK %s", name)
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Bất biến room_quantity có trigger ở CẢ HAI phía")
    void invariantGuardedFromBothSides() throws Exception {
        assertThat(queryForInt("""
                SELECT count(*) FROM pg_trigger
                 WHERE tgname IN ('booking_room_count_check', 'bookings_room_count_check')
                   AND tgconstraint <> 0
                   AND tgdeferrable
                """)).isEqualTo(2);
    }

    private int queryForInt(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
