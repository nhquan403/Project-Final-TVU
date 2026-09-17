package com.tvh.homestay.room.repository;

import com.tvh.homestay.room.entity.RoomClosure;
import java.time.LocalDate;
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

    /**
     * Khoảng còn chặn ít nhất một đêm từ hôm nay trở đi.
     *
     * <p>Lọc theo {@code to_date > today} chứ không trả về tất cả: khoảng đã qua
     * không còn thao tác được gì nữa, và để chúng nằm lại thì sau một mùa khai
     * thác, khoảng đang có hiệu lực nằm cuối một danh sách toàn dòng chết —
     * cùng với một hàng nút "Xoá" không làm gì có ích. Bản ghi vẫn còn trong
     * bảng làm vết lịch sử, chỉ không hiện ở màn hình thao tác.
     *
     * <p>Mốc là {@code to_date} chứ không phải {@code from_date}: khoảng đang
     * diễn ra dở phải còn hiện ra để xoá được khi sửa xong sớm.
     */
    List<RoomClosure> findByRoomIdAndToDateGreaterThanOrderByFromDateAsc(
            Long roomId, LocalDate today);
}
