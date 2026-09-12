package com.tvh.homestay.room;

import com.tvh.homestay.booking.exception.BookingExceptions.BookingNotFound;
import com.tvh.homestay.room.dto.PublicRoomTypeDtos.PublicAmenity;
import com.tvh.homestay.room.dto.PublicRoomTypeDtos.PublicImage;
import com.tvh.homestay.room.dto.PublicRoomTypeDtos.PublicRoomType;
import com.tvh.homestay.room.entity.Amenity;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.entity.RoomTypeImage;
import com.tvh.homestay.room.repository.RoomTypeImageRepository;
import com.tvh.homestay.room.repository.RoomTypeRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Danh mục loại phòng cho trang công khai. */
@Service
public class PublicRoomTypeService {

    private final RoomTypeRepository roomTypes;
    private final RoomTypeImageRepository images;

    public PublicRoomTypeService(RoomTypeRepository roomTypes, RoomTypeImageRepository images) {
        this.roomTypes = roomTypes;
        this.images = images;
    }

    @Transactional(readOnly = true)
    public List<PublicRoomType> list() {
        // Nạp toàn bộ ảnh MỘT lượt rồi gom nhóm trong bộ nhớ. Danh mục chỉ vài
        // loại phòng nên đây là một truy vấn thay cho n truy vấn, không phải
        // tối ưu sớm.
        Map<Long, List<RoomTypeImage>> imagesByType =
                images.findAllByOrderByDisplayOrderAscIdAsc().stream()
                        .collect(Collectors.groupingBy(image -> image.getRoomType().getId()));

        return roomTypes.findActiveWithAmenities().stream()
                .map(type -> toView(type, imagesByType.getOrDefault(type.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicRoomType bySlug(String slug) {
        RoomType type = roomTypes.findActiveBySlug(slug == null ? "" : slug.trim())
                // Dùng chung mã lỗi BOOKING_NOT_FOUND thay vì thêm một mã mới:
                // với khách, "không có loại phòng này" và "đường dẫn sai" là
                // cùng một tình huống — trang 404.
                .orElseThrow(BookingNotFound::new);
        return toView(type, images.findByRoomTypeIdOrderByDisplayOrderAscIdAsc(type.getId()));
    }

    private static PublicRoomType toView(RoomType type, List<RoomTypeImage> typeImages) {
        return new PublicRoomType(
                type.getId(),
                type.getCode(),
                type.getSlug(),
                type.getName(),
                type.getShortDescription(),
                type.getDescription(),
                type.getBasePrice(),
                type.getCapacityAdults(),
                type.getCapacityChildren(),
                type.getBedInfo(),
                type.getAreaSqm(),
                type.getAmenities().stream().map(PublicRoomTypeService::toView).toList(),
                typeImages.stream()
                        .map(image -> new PublicImage(
                                image.getUrl(),
                                // alt rỗng thì lấy tên loại phòng: một ảnh không
                                // có alt là một ảnh trình đọc màn hình bỏ qua.
                                image.getAltText() == null || image.getAltText().isBlank()
                                        ? type.getName()
                                        : image.getAltText(),
                                image.isCover()))
                        .toList());
    }

    private static PublicAmenity toView(Amenity amenity) {
        return new PublicAmenity(
                amenity.getCode(), amenity.getName(), amenity.getIcon(), amenity.getCategory().name());
    }
}
