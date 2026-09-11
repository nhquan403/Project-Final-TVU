package com.tvh.homestay.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.PaymentStatus;
import com.tvh.homestay.booking.repository.BookingRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Xuất CSV — hai thứ Excel làm mà người viết mã không nghĩ tới.
 *
 * <p>Không cần cơ sở dữ liệu: phần đáng kiểm ở đây là cách đóng gói byte, nên
 * kho dữ liệu được thay bằng bản giả và test chạy trong mili giây.
 */
class CsvExportTest {

    @ParameterizedTest
    @ValueSource(strings = {"=", "+", "-", "@", "\t", "\r"})
    @DisplayName("Ô bắt đầu bằng ký tự khởi tạo công thức được vô hiệu hoá bằng tiền tố nháy đơn")
    void formulaTriggersAreNeutralised(String trigger) {
        String cell = CsvExportService.cell(trigger + "cmd|' /C calc'!A0");

        // Ô có thể được bọc trong dấu nháy kép vì cú pháp CSV; điều BẮT BUỘC là
        // ký tự đầu của NỘI DUNG không còn là ký tự khởi tạo công thức.
        String content = cell.startsWith("\"") ? cell.substring(1) : cell;
        assertThat(content)
                .as("bảng tính phải đọc ô này là văn bản, không phải công thức")
                .startsWith("'");
    }

    @Test
    @DisplayName("Nội dung bình thường không bị thêm gì")
    void ordinaryTextIsUntouched() {
        assertThat(CsvExportService.cell("Trần Văn B")).isEqualTo("Trần Văn B");
        assertThat(CsvExportService.cell("TVH8F3K2Q")).isEqualTo("TVH8F3K2Q");
    }

    @Test
    @DisplayName("Dấu phẩy, nháy kép và xuống dòng được bọc đúng cú pháp CSV")
    void csvSyntaxIsEscaped() {
        assertThat(CsvExportService.cell("Nguyễn, Văn A")).isEqualTo("\"Nguyễn, Văn A\"");
        assertThat(CsvExportService.cell("phòng \"view biển\""))
                .isEqualTo("\"phòng \"\"view biển\"\"\"");
        assertThat(CsvExportService.cell("dòng 1\ndòng 2")).isEqualTo("\"dòng 1\ndòng 2\"");
    }

    @Test
    @DisplayName("Tệp mở đầu bằng BOM UTF-8 và giữ nguyên dấu tiếng Việt")
    void fileStartsWithBomAndKeepsVietnamese() {
        BookingRepository repository = mock(BookingRepository.class);
        when(repository.findForExport(any(), any(), any())).thenReturn(List.of(booking()));

        byte[] csv = new CsvExportService(repository).exportBookings(null, null, null);

        assertThat(csv[0]).isEqualTo((byte) 0xEF);
        assertThat(csv[1]).isEqualTo((byte) 0xBB);
        assertThat(csv[2]).isEqualTo((byte) 0xBF);
        // Không có ba byte này, Excel trên Windows đọc tệp bằng bảng mã ANSI và
        // "Phòng Vườn" hiện thành chuỗi rác.
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("Phòng Vườn");
    }

    @Test
    @DisplayName("Tên khách bắt đầu bằng dấu bằng không trở thành công thức trong tệp xuất ra")
    void guestNameStartingWithEqualsIsNeutralisedInTheFile() {
        Booking booking = booking();
        booking.setGuestName("=1+1");
        BookingRepository repository = mock(BookingRepository.class);
        when(repository.findForExport(any(), any(), any())).thenReturn(List.of(booking));

        String csv = new String(
                new CsvExportService(repository).exportBookings(null, null, null),
                StandardCharsets.UTF_8);

        assertThat(csv).contains("'=1+1");
        for (String line : csv.split("\r\n")) {
            assertThat(line)
                    .as("không dòng nào được bắt đầu bằng dấu bằng")
                    .doesNotStartWith("=");
        }
    }

    private static Booking booking() {
        Booking booking = new Booking();
        booking.setCode("TVHTEST01");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.DEPOSIT_PAID);
        booking.setGuestName("Trần Văn B");
        booking.setGuestPhone("0900000001");
        booking.setGuestEmail("b@example.com");
        booking.setRoomTypeNameSnapshot("Phòng Vườn");
        booking.setCheckIn(LocalDate.of(2026, 11, 1));
        booking.setCheckOut(LocalDate.of(2026, 11, 3));
        booking.setRoomQuantity(1);
        booking.setTotalAmount(new BigDecimal("1000000.00"));
        booking.setDepositAmount(new BigDecimal("300000.00"));
        return booking;
    }
}
