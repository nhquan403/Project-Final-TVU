package com.tvh.homestay.availability.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Kết quả tìm phòng trống. */
public final class AvailabilityDtos {

    private AvailabilityDtos() {}

    /** Một loại phòng còn trống trong khoảng ngày được hỏi. */
    public record RoomTypeAvailability(
            Long roomTypeId,
            String code,
            String slug,
            String name,
            String shortDescription,
            String bedInfo,
            int capacityAdults,
            int capacityChildren,
            BigDecimal pricePerNight,
            int nights,
            /** Tổng cả kỳ nghỉ cho số phòng được hỏi. Backend tính, frontend chỉ hiển thị. */
            BigDecimal totalPrice,
            int availableCount,
            String coverImageUrl,
            List<String> topAmenities) {}

    public record AvailabilitySearchResponse(
            LocalDate checkIn, LocalDate checkOut, int nights, List<RoomTypeAvailability> roomTypes) {}

    /**
     * Thông tin một đêm cho lịch chọn ngày.
     *
     * <p>Hình dạng này là HỢP ĐỒNG với {@code ui-date-range-picker} của Phase 2:
     * {@code {price?: number; availableCount: number}}. Đổi tên trường ở đây là
     * làm hỏng việc chặn ngày hết phòng trên lịch.
     */
    public record DayInfo(BigDecimal price, int availableCount) {}

    /**
     * Bản đồ theo ngày, khoá {@code YYYY-MM-DD}.
     *
     * <p>Phải trả ĐỦ mọi ngày trong khoảng được hỏi, kể cả ngày
     * {@code availableCount = 0}: lịch coi ngày KHÔNG có trong bản đồ là ngày
     * không bị chặn, nên bỏ sót một ngày hết phòng là cho khách chọn đúng ngày
     * không đặt được.
     */
    public record DayAvailabilityResponse(Map<String, DayInfo> days) {}
}
