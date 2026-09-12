package com.tvh.homestay.availability;

import com.tvh.homestay.availability.dto.AvailabilityDtos.AvailabilitySearchResponse;
import com.tvh.homestay.availability.dto.AvailabilityDtos.DayAvailabilityResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tìm phòng trống — công khai, không cần đăng nhập. */
@RestController
public class AvailabilityController {

    private final AvailabilityService availability;

    public AvailabilityController(AvailabilityService availability) {
        this.availability = availability;
    }

    @GetMapping("/api/availability")
    public AvailabilitySearchResponse search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "1") int roomQuantity) {
        return availability.search(checkIn, checkOut, adults, children, roomQuantity);
    }

    /**
     * Lịch giá và số phòng trống theo từng ngày, cho lịch chọn ngày ở frontend.
     *
     * <p>Nằm dưới {@code /api/availability} chứ không phải một nhánh riêng, để
     * dùng chung dòng hạn mức tần suất đã khai ở Phase 4.
     */
    @GetMapping("/api/availability/calendar")
    public DayAvailabilityResponse calendar(
            // TUỲ CHỌN: vắng nghĩa là hỏi lịch của toàn homestay.
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availability.dayCalendar(roomTypeId, from, to);
    }
}
