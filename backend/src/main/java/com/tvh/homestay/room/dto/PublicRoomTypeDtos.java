package com.tvh.homestay.room.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hình dạng loại phòng cho TRANG CÔNG KHAI.
 *
 * <p>Cố ý KHÔNG dùng lại {@code AdminDtos.RoomTypeView}. DTO của khu quản trị
 * mang {@code roomCount} (số phòng vật lý — thông tin vận hành nội bộ),
 * {@code publicId} của ảnh trên Cloudinary (định danh để XOÁ ảnh), và cả những
 * loại phòng đang tắt. Trả nguyên nó ra trang bán hàng là để lộ ba thứ mà khách
 * không cần và không nên thấy, chỉ vì tiện tay dùng lại một record.
 */
public final class PublicRoomTypeDtos {

    private PublicRoomTypeDtos() {}

    public record PublicImage(String url, String altText, boolean cover) {}

    /** Tiện ích đã nhóm theo loại — trang chi tiết hiển thị hai nhóm riêng. */
    public record PublicAmenity(String code, String name, String icon, String category) {}

    public record PublicRoomType(
            Long id,
            String code,
            String slug,
            String name,
            String shortDescription,
            String description,
            BigDecimal basePrice,
            int capacityAdults,
            int capacityChildren,
            String bedInfo,
            BigDecimal areaSqm,
            List<PublicAmenity> amenities,
            List<PublicImage> images) {}
}
