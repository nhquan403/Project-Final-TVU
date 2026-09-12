package com.tvh.homestay.availability;

import com.tvh.homestay.availability.dto.AvailabilityDtos.AvailabilitySearchResponse;
import com.tvh.homestay.availability.dto.AvailabilityDtos.DayAvailabilityResponse;
import com.tvh.homestay.availability.dto.AvailabilityDtos.DayInfo;
import com.tvh.homestay.availability.dto.AvailabilityDtos.RoomTypeAvailability;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidBookingRequest;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.repository.RoomTypeImageRepository;
import com.tvh.homestay.room.repository.RoomTypeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tìm phòng trống và dựng lịch giá theo ngày. */
@Service
public class AvailabilityService {

    /** Số đêm tối đa cho một lần đặt. */
    public static final int MAX_NIGHTS = 30;
    /** Số ngày tối đa lịch giá trả về một lượt, để truy vấn không phình ra. */
    private static final int MAX_CALENDAR_DAYS = 120;

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AvailabilityRepository availability;
    private final RoomTypeRepository roomTypes;
    private final RoomTypeImageRepository roomTypeImages;
    private final Clock clock;

    public AvailabilityService(
            AvailabilityRepository availability,
            RoomTypeRepository roomTypes,
            RoomTypeImageRepository roomTypeImages,
            Clock clock) {
        this.availability = availability;
        this.roomTypes = roomTypes;
        this.roomTypeImages = roomTypeImages;
        this.clock = clock;
    }

    /** Hôm nay theo giờ Việt Nam, lấy từ bean Clock của ứng dụng. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Kiểm tra khoảng ngày và sức chứa.
     *
     * <p>Sức chứa so theo TỪNG PHÒNG. So tổng số khách với sức chứa một phòng
     * sẽ từ chối nhầm đơn 4 khách 2 phòng của loại phòng chứa 2 người.
     */
    public void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidBookingRequest("Thiếu ngày nhận hoặc ngày trả phòng.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidBookingRequest("Ngày trả phòng phải sau ngày nhận phòng.");
        }
        if (checkIn.isBefore(today())) {
            throw new InvalidBookingRequest("Ngày nhận phòng không được ở quá khứ.");
        }
        if (nights(checkIn, checkOut) > MAX_NIGHTS) {
            throw new InvalidBookingRequest("Một lần đặt tối đa " + MAX_NIGHTS + " đêm.");
        }
    }

    public static int nights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Số khách mỗi phòng, làm tròn LÊN — 3 khách 2 phòng cần loại phòng chứa 2. */
    public static int perRoom(int guests, int roomQuantity) {
        return (int) Math.ceil((double) guests / Math.max(1, roomQuantity));
    }

    @Transactional(readOnly = true)
    public AvailabilitySearchResponse search(
            LocalDate checkIn, LocalDate checkOut, int adults, int children, int roomQuantity) {
        validateDates(checkIn, checkOut);
        if (adults < 1) {
            throw new InvalidBookingRequest("Phải có ít nhất một người lớn.");
        }
        if (roomQuantity < 1) {
            throw new InvalidBookingRequest("Số phòng phải từ 1 trở lên.");
        }

        int nights = nights(checkIn, checkOut);
        List<RoomTypeAvailability> result = availability
                .findFreeRoomTypes(
                        checkIn,
                        checkOut,
                        perRoom(adults, roomQuantity),
                        perRoom(children, roomQuantity),
                        roomQuantity)
                .stream()
                .map(row -> toDto(row, nights, roomQuantity))
                .toList();

        return new AvailabilitySearchResponse(checkIn, checkOut, nights, result);
    }

    /**
     * Lịch giá và số phòng trống theo từng ngày, cho lịch chọn ngày ở frontend.
     *
     * <p>Trả ĐỦ mọi ngày trong khoảng, kể cả ngày hết phòng: lịch coi ngày
     * vắng mặt trong bản đồ là ngày không bị chặn.
     */
    @Transactional(readOnly = true)
    public DayAvailabilityResponse dayCalendar(Long roomTypeId, LocalDate from, LocalDate to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new InvalidBookingRequest("Khoảng ngày của lịch giá không hợp lệ.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_CALENDAR_DAYS) {
            throw new InvalidBookingRequest("Lịch giá tối đa " + MAX_CALENDAR_DAYS + " ngày một lượt.");
        }
        Map<String, DayInfo> days = new LinkedHashMap<>();

        if (roomTypeId == null) {
            // Lịch của TOÀN HOMESTAY, dùng cho thanh tìm phòng ở trang chủ —
            // lúc đó khách chưa chọn loại phòng nên chưa có roomTypeId để hỏi.
            // Một đêm chỉ bị chặn khi MỌI loại phòng đều hết chỗ.
            availability.countFreeRoomsPerNight(from, to).forEach(night ->
                    days.put(
                            night.night().format(DAY_KEY),
                            new DayInfo(night.price(), night.freeRooms())));
            return new DayAvailabilityResponse(days);
        }

        RoomType roomType = roomTypes.findById(roomTypeId)
                .orElseThrow(() -> new InvalidBookingRequest("Không tìm thấy loại phòng."));

        for (LocalDate night = from; night.isBefore(to); night = night.plusDays(1)) {
            days.put(
                    night.format(DAY_KEY),
                    new DayInfo(
                            // Cùng một con số cho mọi đêm: chưa có bảng giá theo
                            // đêm trong schema. Giao diện vì thế KHÔNG được vẽ
                            // nó như một mức giá thay đổi theo ngày.
                            roomType.getBasePrice(),
                            availability.countFreeRoomsForNight(roomTypeId, night)));
        }
        return new DayAvailabilityResponse(days);
    }

    private RoomTypeAvailability toDto(
            AvailabilityRepository.FreeRoomTypeRow row, int nights, int roomQuantity) {
        BigDecimal total = row.basePrice()
                .multiply(BigDecimal.valueOf((long) nights * roomQuantity));
        RoomType entity = roomTypes.findById(row.roomTypeId()).orElseThrow();
        return new RoomTypeAvailability(
                row.roomTypeId(),
                row.code(),
                row.slug(),
                row.name(),
                row.shortDescription(),
                row.bedInfo(),
                row.capacityAdults(),
                row.capacityChildren(),
                row.basePrice(),
                nights,
                total,
                row.availableCount(),
                coverImageUrlOf(row.roomTypeId()),
                entity.getAmenities().stream().map(a -> a.getName()).limit(3).toList());
    }

    private String coverImageUrlOf(Long roomTypeId) {
        return roomTypeImages
                .findFirstByRoomTypeIdOrderByCoverDescDisplayOrderAsc(roomTypeId)
                .map(image -> image.getUrl())
                .orElse(null);
    }
}
