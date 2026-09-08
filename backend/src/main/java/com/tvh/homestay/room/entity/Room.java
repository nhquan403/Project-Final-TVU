package com.tvh.homestay.room.entity;

import com.tvh.homestay.common.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Phòng vật lý. Đây là thứ ràng buộc chống trùng lịch đặt lên. */
@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(columnDefinition = "text")
    private String note;
}
