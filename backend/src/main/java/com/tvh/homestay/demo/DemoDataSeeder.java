package com.tvh.homestay.demo;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

/**
 * Nạp dữ liệu mẫu cho profile {@code demo}.
 *
 * <h2>Vì sao KHÔNG dùng Flyway</h2>
 *
 * Đặt seed thành một migration (kiểu {@code V900__demo_data.sql} chạy có điều
 * kiện) hỏng theo ba hướng:
 *
 * <ol>
 *   <li>Chạy profile {@code demo} rồi đổi sang {@code prod} trên CÙNG volume:
 *       Flyway thấy migration đã áp dụng trong {@code flyway_schema_history}
 *       nhưng không resolve được trên classpath, {@code validateOnMigrate} chặn
 *       khởi động, và container rơi vào vòng restart vô tận.
 *   <li>"Rollback bằng cách revert commit" không đúng: revert xoá tệp khỏi
 *       classpath trong khi cơ sở dữ liệu vẫn ghi nhận nó — gây đúng lỗi trên.
 *   <li>Seed không thuộc về lược đồ. Lịch sử schema và dữ liệu trình diễn là
 *       hai vòng đời khác nhau; trộn chúng làm cả hai khó suy luận.
 * </ol>
 *
 * <p>Là {@code ApplicationRunner} nên nó chạy SAU khi Flyway di trú xong và
 * sau khi context sẵn sàng. Đổi profile không để lại dấu vết nào trong
 * {@code flyway_schema_history}.
 *
 * <h2>Idempotent</h2>
 *
 * Seeder chạy mỗi lần container khởi động, kể cả khi dữ liệu đã có. Mọi câu
 * lệnh trong {@code demo-data.sql} đều bỏ qua bản ghi đã tồn tại, và tài khoản
 * quản trị chỉ được tạo khi chưa có.
 *
 * <h2>Mật khẩu quản trị</h2>
 *
 * Sinh ngẫu nhiên lúc chạy và chỉ in ra log container. KHÔNG nằm trong repo,
 * không trong {@code .env}, không trong tài liệu. Tài khoản bật sẵn cờ
 * {@code must_change_password}, nên mật khẩu tạm này hết giá trị ngay sau lần
 * đăng nhập đầu tiên — {@code MustChangePasswordFilter} chặn mọi thao tác khác
 * cho tới khi đổi xong.
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String ADMIN_EMAIL = "admin@tvh.local";
    private static final String SEED_SCRIPT = "db/seed/demo-data.sql";

    /**
     * Bảng chữ cái sinh mật khẩu.
     *
     * <p>Bỏ những ký tự dễ đọc nhầm khi chép từ log ra bàn phím: chữ O và số 0,
     * chữ l thường và số 1. Một mật khẩu tạm mà người ta gõ sai ba lần thì
     * chẳng khác gì không có mật khẩu nào.
     */
    private static final String ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private static final int PASSWORD_LENGTH = 16;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedAdmin();
        runSeedScript();
    }

    private void seedAdmin() {
        Integer existing = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?", Integer.class, ADMIN_EMAIL);

        if (existing != null && existing > 0) {
            // Đã có tài khoản thì KHÔNG đổi mật khẩu và KHÔNG in lại gì. In lại
            // một mật khẩu không còn đúng sẽ khiến người đọc log gõ nhầm mãi;
            // đổi mật khẩu sau mỗi lần khởi động lại còn tệ hơn — nó đá văng
            // quản trị viên đang dùng dở.
            log.info("Tài khoản quản trị demo đã tồn tại — giữ nguyên mật khẩu hiện tại.");
            return;
        }

        String password = randomPassword();

        jdbc.update("""
                INSERT INTO users (email, password_hash, full_name, phone, role,
                                   enabled, must_change_password)
                VALUES (?, ?, ?, ?, 'ADMIN', true, true)
                ON CONFLICT (email) DO NOTHING
                """,
                ADMIN_EMAIL, passwordEncoder.encode(password),
                "Quản trị viên demo", "02943855246");

        // Dòng log này là đường DUY NHẤT để biết mật khẩu. Định dạng cố ý dễ
        // tìm bằng grep — xem docs/cai-dat.md.
        log.warn("""

                ════════════════════════════════════════════════════════════
                 Mat khau admin demo (chi hien mot lan, chi trong log nay)
                   Email    : {}
                   Mat khau : {}
                 Tai khoan bat buoc doi mat khau o lan dang nhap dau tien.
                ════════════════════════════════════════════════════════════
                """, ADMIN_EMAIL, password);
    }

    /**
     * Chạy toàn bộ tệp SQL bằng MỘT lệnh.
     *
     * <p>Không dùng {@code ScriptUtils}: nó cắt tệp theo dấu chấm phẩy mà không
     * hiểu chuỗi trích dẫn kiểu đô-la, nên nó sẽ xé đôi khối {@code DO $seed$
     * ... $seed$} sinh đơn đặt phòng. Gửi cả tệp cho máy chủ để chính PostgreSQL
     * phân tích cú pháp, và nhân tiện được luôn tính nguyên tử: một lệnh nhiều
     * câu chạy trong một giao dịch ngầm, nên không có trạng thái "chạy dở".
     */
    private void runSeedScript() throws Exception {
        String sql;
        try (var reader = new java.io.InputStreamReader(
                new ClassPathResource(SEED_SCRIPT).getInputStream(), StandardCharsets.UTF_8)) {
            sql = FileCopyUtils.copyToString(reader);
        }

        jdbc.execute(sql);

        Integer bookings = jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE code LIKE 'TVHDEMO%'", Integer.class);
        Integer rooms = jdbc.queryForObject("SELECT count(*) FROM rooms", Integer.class);
        log.info("Dữ liệu mẫu sẵn sàng: {} phòng, {} đơn đặt phòng mẫu.", rooms, bookings);
    }

    private String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder out = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            out.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return out.toString();
    }
}
