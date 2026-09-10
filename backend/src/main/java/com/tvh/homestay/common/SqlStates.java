package com.tvh.homestay.common;

import java.sql.SQLException;

/**
 * Nhận diện mã lỗi SQLSTATE gốc của PostgreSQL bên dưới các lớp bọc của Spring.
 *
 * <p><b>Vì sao cần lớp này.</b> Vòng thử gán phòng chỉ được thử lại khi đụng
 * đúng ràng buộc chống trùng lịch. Bắt chung mọi
 * {@code DataIntegrityViolationException} sẽ khiến một lỗi khoá ngoại — tức là
 * bug thật trong mã — bị báo cho khách thành "hết phòng", và bug đó không bao
 * giờ được ai phát hiện.
 */
public final class SqlStates {

    /** Vi phạm ràng buộc EXCLUDE. Đây là mã duy nhất đáng thử lại. */
    public static final String EXCLUSION_VIOLATION = "23P01";

    private SqlStates() {}

    public static boolean isExclusionViolation(Throwable thrown) {
        return EXCLUSION_VIOLATION.equals(sqlStateOf(thrown));
    }

    /** Lần theo chuỗi nguyên nhân tìm {@link SQLException} đầu tiên. */
    public static String sqlStateOf(Throwable thrown) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }
}
