package com.tvh.homestay.room.entity;

import com.tvh.homestay.common.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Loại phòng — thứ khách chọn khi đặt.
 *
 * <p>Phân biệt với {@link Room} (phòng vật lý) là quyết định nền của hệ thống:
 * khách đặt một loại phòng và một số lượng, hệ thống mới gán phòng vật lý cụ
 * thể, và ràng buộc chống trùng lịch đặt trên phòng vật lý.
 */
@Entity
@Table(name = "room_types")
@Getter
@Setter
public class RoomType extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "short_description", columnDefinition = "text")
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    /** NUMERIC(12,2) — không bao giờ dùng double cho tiền. */
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "capacity_adults", nullable = false)
    private int capacityAdults;

    @Column(name = "capacity_children", nullable = false)
    private int capacityChildren = 0;

    @Column(name = "bed_info", length = 150)
    private String bedInfo;

    @Column(name = "area_sqm", precision = 6, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Bảng nối {@code room_type_amenities} chỉ có hai cột khoá và không mang
     * dữ liệu riêng, nên ánh xạ thẳng bằng quan hệ thay vì dựng một lớp
     * entity chỉ để chứa hai khoá ngoại. {@code ddl-auto=validate} vẫn kiểm
     * bảng nối này như mọi bảng khác.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_type_amenities",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new LinkedHashSet<>();
}
