package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureResult;
import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureView;
import com.tvh.homestay.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Khoảng ngày một phòng không nhận khách.
 *
 * <p>Lồng dưới tiền tố {@code /api/admin/rooms} đã có chứ không mở nhóm
 * {@code /api/admin/closures} riêng: khoảng đóng không tồn tại độc lập với
 * phòng, và lồng vào tài nguyên cha thì ma trận phân quyền của
 * {@code EndpointAuthorizationIT} không phải thêm dòng nào.
 */
@RestController
public class AdminRoomClosureController {

    private final AdminRoomService rooms;

    public AdminRoomClosureController(AdminRoomService rooms) {
        this.rooms = rooms;
    }

    @GetMapping("/api/admin/rooms/{id}/closures")
    public List<RoomClosureView> list(@PathVariable Long id) {
        return rooms.listClosures(id);
    }

    /** Phản hồi kèm danh sách đơn giao nhau với khoảng vừa đóng. KHÔNG huỷ đơn nào. */
    @PostMapping("/api/admin/rooms/{id}/closures")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomClosureResult add(
            @PathVariable Long id,
            @Valid @RequestBody RoomClosureRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return rooms.addClosure(id, request, CurrentAdmin.idOf(principal));
    }

    @DeleteMapping("/api/admin/rooms/closures/{closureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long closureId) {
        rooms.removeClosure(closureId);
    }
}
