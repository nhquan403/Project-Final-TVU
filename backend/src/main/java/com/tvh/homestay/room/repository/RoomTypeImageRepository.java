package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.RoomTypeImage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Ảnh của loại phòng. */
public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImage, Long> {

    /** Ảnh bìa nếu có, nếu không thì ảnh đầu tiên theo thứ tự hiển thị. */
    Optional<RoomTypeImage> findFirstByRoomTypeIdOrderByCoverDescDisplayOrderAsc(Long roomTypeId);
}
