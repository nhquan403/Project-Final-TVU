package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.RoomTypeImage;
import org.springframework.data.jpa.repository.JpaRepository;

/** Ảnh của loại phòng. */
public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {
}
