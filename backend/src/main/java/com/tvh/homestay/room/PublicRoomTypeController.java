package com.tvh.homestay.room;

import com.tvh.homestay.room.dto.PublicRoomTypeDtos.PublicRoomType;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh mục loại phòng cho trang công khai.
 *
 * <p>Đường dẫn này đã được khai {@code permitAll} trong {@code SecurityConfig}
 * từ Phase 4 và nằm trong ma trận phân quyền, nhưng tới Phase 8 mới có lớp cài
 * đặt — trước đó mọi lượt gọi trả 404. Đó là lý do một dòng trong ma trận phân
 * quyền không chứng minh được endpoint có thật.
 */
@RestController
public class PublicRoomTypeController {

    private final PublicRoomTypeService roomTypes;

    public PublicRoomTypeController(PublicRoomTypeService roomTypes) {
        this.roomTypes = roomTypes;
    }

    @GetMapping("/api/room-types")
    public List<PublicRoomType> list() {
        return roomTypes.list();
    }

    @GetMapping("/api/room-types/{slug}")
    public PublicRoomType bySlug(@PathVariable String slug) {
        return roomTypes.bySlug(slug);
    }
}
