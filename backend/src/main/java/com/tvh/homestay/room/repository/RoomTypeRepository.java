package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Loại phòng — thứ khách chọn khi đặt. */
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    /**
     * Nạp kèm tiện ích bằng {@code join fetch}.
     *
     * <p>{@code open-in-view} đã tắt, nên đọc {@code roomType.getAmenities()}
     * sau khi transaction đóng sẽ ném {@code LazyInitializationException}.
     */
    @org.springframework.data.jpa.repository.Query(
            "select distinct t from RoomType t left join fetch t.amenities order by t.displayOrder, t.id")
    java.util.List<RoomType> findAllWithAmenities();

    /** Chỉ loại phòng ĐANG bán — trang công khai không bao giờ thấy loại đã tắt. */
    @org.springframework.data.jpa.repository.Query(
            "select distinct t from RoomType t left join fetch t.amenities"
                    + " where t.active = true order by t.displayOrder, t.id")
    java.util.List<RoomType> findActiveWithAmenities();

    @org.springframework.data.jpa.repository.Query(
            "select distinct t from RoomType t left join fetch t.amenities"
                    + " where t.active = true and lower(t.slug) = lower(:slug)")
    java.util.Optional<RoomType> findActiveBySlug(
            @org.springframework.data.repository.query.Param("slug") String slug);

    @org.springframework.data.jpa.repository.Query(
            "select distinct t from RoomType t left join fetch t.amenities where t.id = :id")
    java.util.Optional<RoomType> findWithAmenities(
            @org.springframework.data.repository.query.Param("id") Long id);

    java.util.Optional<RoomType> findByCodeIgnoreCase(String code);

    java.util.Optional<RoomType> findBySlugIgnoreCase(String slug);
}
