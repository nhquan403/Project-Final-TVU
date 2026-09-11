package com.tvh.homestay.report;

import com.tvh.homestay.admin.AdminPaymentService;
import com.tvh.homestay.report.dto.DashboardSummary.MetricAxes;
import com.tvh.homestay.report.dto.DashboardSummary.MonthlyValue;
import com.tvh.homestay.report.dto.DashboardSummary.Summary;
import com.tvh.homestay.report.dto.DashboardSummary.TopRoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gom bảy chỉ số của dashboard thành MỘT phản hồi. */
@Service
public class DashboardService {

    /** Số tháng mặc định khi client không truyền kỳ. */
    private static final int DEFAULT_MONTHS = 12;

    private static final int TOP_ROOM_TYPES = 5;

    private final RevenueReportRepository reports;
    private final AdminPaymentService payments;
    private final Clock clock;

    public DashboardService(
            RevenueReportRepository reports, AdminPaymentService payments, Clock clock) {
        this.reports = reports;
        this.payments = payments;
        this.clock = clock;
    }

    /**
     * @param from ngày đầu kỳ, bao gồm; {@code null} = mùng 1 của 12 tháng trước
     * @param to ngày cuối kỳ, KHÔNG bao gồm; {@code null} = mùng 1 tháng sau
     */
    @Transactional(readOnly = true)
    public Summary summary(LocalDate from, LocalDate to) {
        LocalDate periodEnd = to != null ? to : LocalDate.now(clock).withDayOfMonth(1).plusMonths(1);
        LocalDate periodStart = from != null
                ? from.withDayOfMonth(1)
                : periodEnd.minusMonths(DEFAULT_MONTHS);

        List<MonthlyValue> bookingValue = reports.bookingValueByMonth(periodStart, periodEnd);
        List<MonthlyValue> received = reports.amountReceivedByMonth(periodStart, periodEnd);
        List<MonthlyValue> occupancy = reports.occupancyByMonth(periodStart, periodEnd);
        List<MonthlyValue> newBookings = reports.newBookingsByMonth(periodStart, periodEnd);
        List<TopRoomType> topRoomTypes =
                reports.topRoomTypes(periodStart, periodEnd, TOP_ROOM_TYPES);

        long created = reports.countCreated(periodStart, periodEnd, List.of());
        long cancelled =
                reports.countCreated(periodStart, periodEnd, List.of("CANCELLED", "NO_SHOW"));

        return new Summary(
                periodStart,
                periodEnd,
                reports.sum(bookingValue),
                reports.sum(received),
                created,
                cancelled,
                ratio(cancelled, created),
                averageOccupancy(occupancy),
                payments.pendingCount(),
                bookingValue,
                received,
                occupancy,
                newBookings,
                topRoomTypes,
                MetricAxes.standard());
    }

    private static BigDecimal ratio(long part, long whole) {
        if (whole == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .divide(BigDecimal.valueOf(whole), 4, RoundingMode.HALF_UP);
    }

    /**
     * Trung bình CỘNG của tỉ lệ từng tháng, không phải tổng đêm chia tổng sức
     * chứa.
     *
     * <p>Hai cách cho ra hai con số khác nhau khi các tháng có số ngày khác
     * nhau. Trung bình cộng khớp với thứ người dùng nhìn thấy trên biểu đồ —
     * và một con số tóm tắt không khớp với biểu đồ ngay bên cạnh nó là con số
     * gây mất tin tưởng vào cả trang.
     */
    private static BigDecimal averageOccupancy(List<MonthlyValue> monthly) {
        if (monthly.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = monthly.stream()
                .map(MonthlyValue::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(monthly.size()), 4, RoundingMode.HALF_UP);
    }
}
