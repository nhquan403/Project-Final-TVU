package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Loại phòng — thứ khách chọn khi đặt. */
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
}
