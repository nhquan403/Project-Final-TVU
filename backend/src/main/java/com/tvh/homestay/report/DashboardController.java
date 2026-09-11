package com.tvh.homestay.report;

import com.tvh.homestay.report.dto.DashboardSummary.Summary;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Số liệu tổng hợp cho trang chủ khu quản trị.
 *
 * <p>{@code from} và {@code to} là hợp đồng đối chiếu: bước Verify của Phase 7
 * so kết quả ở đây với truy vấn SQL thô chạy bằng ĐÚNG hai tham số này. Không
 * truyền thì mặc định 12 tháng gần nhất — và khi đó hai bên chỉ khớp nếu câu
 * SQL kia cũng dùng đúng kỳ ấy.
 */
@RestController
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/api/admin/dashboard")
    public Summary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboard.summary(from, to);
    }
}
