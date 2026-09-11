package com.tvh.homestay.report;

import com.tvh.homestay.availability.AvailabilityService;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.repository.BookingRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xuất danh sách đơn ra CSV.
 *
 * <h2>Hai thứ dễ bỏ sót, cả hai đều gây hậu quả thật</h2>
 *
 * <p><b>BOM UTF-8.</b> Excel trên Windows đọc CSV không có BOM bằng bảng mã
 * ANSI của hệ thống, nên "Phòng Vườn" hiện thành "PhÃ²ng Vá»n". Ba byte BOM
 * là cách duy nhất nói cho Excel biết tệp là UTF-8 mà không cần người dùng bấm
 * qua hộp thoại nhập dữ liệu.
 *
 * <p><b>Chống chèn công thức.</b> Ô bắt đầu bằng {@code =}, {@code +},
 * {@code -}, {@code @}, tab hay CR được Excel và LibreOffice hiểu là CÔNG THỨC.
 * {@code guest_name} và {@code special_request} do khách ẩn danh nhập, nên một
 * khách đặt tên là {@code =cmd|' /C calc'!A0} và một quản trị viên mở tệp xuất
 * ra là đủ để chạy lệnh trên máy quản trị viên. Tiền tố dấu nháy đơn khiến
 * bảng tính coi nội dung là văn bản; giá trị vẫn đọc được nguyên vẹn.
 */
@Service
public class CsvExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Ba byte mở đầu nói với Excel rằng tệp này là UTF-8. */
    static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final String[] HEADERS = {
        "ma_don", "trang_thai", "trang_thai_thanh_toan", "ten_khach", "dien_thoai", "email",
        "loai_phong", "nhan_phong", "tra_phong", "so_dem", "so_phong", "tong_tien",
        "tien_coc", "ngay_tao", "yeu_cau_dac_biet"
    };

    private final BookingRepository bookings;

    public CsvExportService(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public byte[] exportBookings(String status, LocalDate from, LocalDate to) {
        BookingStatus parsed = status == null || status.isBlank()
                ? null
                : BookingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        List<Booking> rows = bookings.findForExport(parsed, from, to);

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", HEADERS)).append("\r\n");
        for (Booking booking : rows) {
            csv.append(line(booking)).append("\r\n");
        }

        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, withBom, UTF8_BOM.length, body.length);
        return withBom;
    }

    private static String line(Booking booking) {
        return String.join(",",
                cell(booking.getCode()),
                cell(booking.getStatus().name()),
                cell(booking.getPaymentStatus().name()),
                cell(booking.getGuestName()),
                cell(booking.getGuestPhone()),
                cell(booking.getGuestEmail()),
                cell(booking.getRoomTypeNameSnapshot()),
                cell(booking.getCheckIn().format(DATE)),
                cell(booking.getCheckOut().format(DATE)),
                cell(String.valueOf(
                        AvailabilityService.nights(booking.getCheckIn(), booking.getCheckOut()))),
                cell(String.valueOf(booking.getRoomQuantity())),
                cell(booking.getTotalAmount().toPlainString()),
                cell(booking.getDepositAmount().toPlainString()),
                cell(booking.getCreatedAt() == null ? "" : booking.getCreatedAt().format(DATE_TIME)),
                cell(booking.getSpecialRequest()));
    }

    /**
     * Một ô CSV an toàn cho cả bộ đọc CSV lẫn bảng tính.
     *
     * <p>Hai lớp tách biệt: tiền tố {@code '} vô hiệu hoá công thức, còn dấu
     * nháy kép bao ngoài lo phần cú pháp CSV (dấu phẩy, xuống dòng, nháy kép
     * trong nội dung). Thiếu lớp nào cũng hỏng theo một kiểu riêng.
     */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && isFormulaTrigger(safe.charAt(0))) {
            safe = "'" + safe;
        }
        if (safe.contains("\"") || safe.contains(",") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static boolean isFormulaTrigger(char first) {
        return first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r';
    }
}
