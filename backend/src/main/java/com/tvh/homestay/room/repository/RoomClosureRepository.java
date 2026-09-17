package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.RoomClosure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Khoảng đóng phòng.
 *
 * <p>Chỉ có truy vấn liệt kê: việc trừ phòng đang đóng khỏi kho bán nằm ở
 * {@code AvailabilityRepository} bằng SQL thuần, vì nó cần toán tử
 * {@code &&} trên {@code daterange} — thứ JPQL không có.
 */
public interface RoomClosureRepository extends JpaRepository<RoomClosure, Long> {

    List<RoomClosure> findByRoomIdOrderByFromDateAsc(Long roomId);
}
