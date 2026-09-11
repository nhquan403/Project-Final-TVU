package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.RoomRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomStatusRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomStatusResult;
import com.tvh.homestay.admin.dto.AdminDtos.RoomView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD phòng vật lý. */
@RestController
public class AdminRoomController {

    private final AdminRoomService rooms;

    public AdminRoomController(AdminRoomService rooms) {
        this.rooms = rooms;
    }

    @GetMapping("/api/admin/rooms")
    public List<RoomView> list(@RequestParam(required = false) Long roomTypeId) {
        return rooms.list(roomTypeId);
    }

    @PostMapping("/api/admin/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView create(@Valid @RequestBody RoomRequest request) {
        return rooms.create(request);
    }

    @PutMapping("/api/admin/rooms/{id}")
    public RoomView update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return rooms.update(id, request);
    }

    /** Đổi trạng thái vận hành. Phản hồi kèm danh sách đơn bị ảnh hưởng, KHÔNG tự huỷ đơn nào. */
    @PatchMapping("/api/admin/rooms/{id}")
    public RoomStatusResult changeStatus(
            @PathVariable Long id, @Valid @RequestBody RoomStatusRequest request) {
        return rooms.changeStatus(id, request.status());
    }

    @DeleteMapping("/api/admin/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        rooms.delete(id);
    }
}
