package com.tvh.homestay.report;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Xuất báo cáo cho khu quản trị. */
@RestController
public class ReportController {

    private final CsvExportService csv;

    public ReportController(CsvExportService csv) {
        this.csv = csv;
    }

    @GetMapping("/api/admin/reports/bookings.csv")
    public ResponseEntity<byte[]> bookingsCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] body = csv.exportBookings(status, from, to);
        return ResponseEntity.ok()
                // charset=UTF-8 nói với trình duyệt; BOM trong nội dung nói với
                // Excel. Cần cả hai vì Excel không đọc header HTTP.
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookings.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
