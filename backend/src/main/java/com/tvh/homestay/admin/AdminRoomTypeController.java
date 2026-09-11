package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.AddImageRequest;
import com.tvh.homestay.admin.dto.AdminDtos.AmenityIdsRequest;
import com.tvh.homestay.admin.dto.AdminDtos.AmenityView;
import com.tvh.homestay.admin.dto.AdminDtos.ImageOrderRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomTypeRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomTypeView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD loại phòng, tiện ích và thư viện ảnh. */
@RestController
public class AdminRoomTypeController {

    private final AdminRoomTypeService roomTypes;

    public AdminRoomTypeController(AdminRoomTypeService roomTypes) {
        this.roomTypes = roomTypes;
    }

    @GetMapping("/api/admin/room-types")
    public List<RoomTypeView> list() {
        return roomTypes.list();
    }

    @GetMapping("/api/admin/room-types/{id}")
    public RoomTypeView detail(@PathVariable Long id) {
        return roomTypes.detail(id);
    }

    @PostMapping("/api/admin/room-types")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeView create(@Valid @RequestBody RoomTypeRequest request) {
        return roomTypes.create(request);
    }

    @PutMapping("/api/admin/room-types/{id}")
    public RoomTypeView update(@PathVariable Long id, @Valid @RequestBody RoomTypeRequest request) {
        return roomTypes.update(id, request);
    }

    @DeleteMapping("/api/admin/room-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roomTypes.delete(id);
    }

    @PutMapping("/api/admin/room-types/{id}/amenities")
    public RoomTypeView setAmenities(
            @PathVariable Long id, @Valid @RequestBody AmenityIdsRequest request) {
        return roomTypes.setAmenities(id, request.amenityIds());
    }

    @PostMapping("/api/admin/room-types/{id}/images")
    public RoomTypeView addImage(@PathVariable Long id, @Valid @RequestBody AddImageRequest request) {
        return roomTypes.addImage(id, request);
    }

    @PutMapping("/api/admin/room-types/{id}/images")
    public RoomTypeView reorderImages(
            @PathVariable Long id, @Valid @RequestBody ImageOrderRequest request) {
        return roomTypes.reorderImages(id, request.images());
    }

    @DeleteMapping("/api/admin/room-types/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable Long imageId) {
        roomTypes.deleteImage(imageId);
    }

    @GetMapping("/api/admin/amenities")
    public List<AmenityView> amenities() {
        return roomTypes.allAmenities();
    }
}
