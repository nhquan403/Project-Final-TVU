package com.tvh.homestay.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Số liệu tổng hợp của trang dashboard. */
public final class DashboardSummary {

    private DashboardSummary() {}

    /** Một điểm dữ liệu theo tháng. {@code month} luôn là ngày mùng 1. */
    public record MonthlyValue(LocalDate month, BigDecimal value) {}

    public record TopRoomType(String roomTypeName, long bookings, BigDecimal bookingValue) {}

    /**
     * Ba chỉ số dùng BA TRỤC THỜI GIAN khác nhau.
     *
     * <p>Đưa lời giải thích này vào chính phản hồi API, không chỉ để trong tài
     * liệu: hai con số nằm cạnh nhau trên màn hình mà không nói rõ trục thời
     * gian thì người đọc mặc định hiểu là cùng kỳ, và sẽ kết luận sai.
     */
    public record MetricAxes(
            String bookingValue, String amountReceived, String occupancy, String newBookings) {

        public static MetricAxes standard() {
            return new MetricAxes(
                    "Tính theo ngày NHẬN PHÒNG (check_in)",
                    "Tính theo ngày TIỀN VỀ (paid_at)",
                    "Tính theo số đêm-phòng đã bán nằm trong tháng",
                    "Tính theo ngày TẠO ĐƠN (created_at)");
        }
    }

    /**
     * Toàn bộ dashboard trong một phản hồi.
     *
     * <p>Hai chỉ số tiền cố ý mang hai cái tên khác nhau và KHÔNG cái nào tên
     * là "doanh thu": hệ thống chỉ thu 30% tiền cọc, nên
     * {@code bookingValueTotal} là giá trị các đơn đã chốt, còn
     * {@code amountReceivedTotal} mới là tiền thực sự đã về tài khoản.
     */
    public record Summary(
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal bookingValueTotal,
            BigDecimal amountReceivedTotal,
            long newBookings,
            long cancelledBookings,
            BigDecimal cancellationRate,
            BigDecimal occupancyRate,
            long reconcileCount,
            List<MonthlyValue> bookingValueByMonth,
            List<MonthlyValue> amountReceivedByMonth,
            List<MonthlyValue> occupancyByMonth,
            List<MonthlyValue> newBookingsByMonth,
            List<TopRoomType> topRoomTypes,
            MetricAxes axes) {}
}
