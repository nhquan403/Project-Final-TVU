package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tiện ích phòng và tiện ích homestay. */
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}
