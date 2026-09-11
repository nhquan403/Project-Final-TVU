package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

/** Phòng vật lý — thứ ràng buộc chống trùng lịch đặt lên. */
public interface RoomRepository extends JpaRepository<Room, Long> {

    java.util.List<Room> findAllByOrderByRoomNumberAsc();

    java.util.List<Room> findByRoomTypeIdOrderByRoomNumberAsc(Long roomTypeId);

    long countByRoomTypeId(Long roomTypeId);

    boolean existsByRoomNumberIgnoreCase(String roomNumber);
}
