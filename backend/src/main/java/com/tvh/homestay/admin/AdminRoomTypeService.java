package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.AddImageRequest;
import com.tvh.homestay.admin.dto.AdminDtos.AmenityView;
import com.tvh.homestay.admin.dto.AdminDtos.ImageOrderEntry;
import com.tvh.homestay.admin.dto.AdminDtos.RoomTypeImageView;
import com.tvh.homestay.admin.dto.AdminDtos.RoomTypeRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomTypeView;
import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.admin.exception.AdminExceptions.ResourceInUse;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.room.entity.Amenity;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.entity.RoomTypeImage;
import com.tvh.homestay.room.repository.AmenityRepository;
import com.tvh.homestay.room.repository.RoomRepository;
import com.tvh.homestay.room.repository.RoomTypeImageRepository;
import com.tvh.homestay.room.repository.RoomTypeRepository;
import com.tvh.homestay.storage.ImageStorageService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD loại phòng, tiện ích đi kèm, và thư viện ảnh của loại phòng. */
@Service
public class AdminRoomTypeService {

    private final RoomTypeRepository roomTypes;
    private final RoomTypeImageRepository images;
    private final AmenityRepository amenities;
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final ImageStorageService storage;

    public AdminRoomTypeService(
            RoomTypeRepository roomTypes,
            RoomTypeImageRepository images,
            AmenityRepository amenities,
            RoomRepository rooms,
            BookingRepository bookings,
            ImageStorageService storage) {
        this.roomTypes = roomTypes;
        this.images = images;
        this.amenities = amenities;
        this.rooms = rooms;
        this.bookings = bookings;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<RoomTypeView> list() {
        List<RoomType> all = roomTypes.findAllWithAmenities();
        // Đếm phòng cho TẤT CẢ loại trong một truy vấn thay vì một truy vấn mỗi
        // dòng: bảng này hiện toàn bộ danh mục, nên n+1 ở đây là n+1 thật.
        Map<Long, Long> roomCounts = rooms.findAll().stream()
                .collect(Collectors.groupingBy(
                        room -> room.getRoomType().getId(), Collectors.counting()));
        return all.stream().map(type -> toView(type, roomCounts.getOrDefault(type.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public RoomTypeView detail(Long id) {
        return toView(require(id), rooms.countByRoomTypeId(id));
    }

    @Transactional(readOnly = true)
    public List<AmenityView> allAmenities() {
        return amenities.findAllByOrderByCategoryAscDisplayOrderAsc().stream()
                .map(AdminRoomTypeService::toAmenityView)
                .toList();
    }

    @Transactional
    public RoomTypeView create(RoomTypeRequest request) {
        roomTypes.findByCodeIgnoreCase(request.code()).ifPresent(existing -> {
            throw new InvalidAdminRequest("Mã loại phòng " + request.code() + " đã tồn tại.");
        });
        roomTypes.findBySlugIgnoreCase(request.slug()).ifPresent(existing -> {
            throw new InvalidAdminRequest("Đường dẫn " + request.slug() + " đã được dùng.");
        });
        RoomType type = new RoomType();
        apply(type, request);
        roomTypes.save(type);
        return toView(type, 0);
    }

    @Transactional
    public RoomTypeView update(Long id, RoomTypeRequest request) {
        RoomType type = require(id);
        roomTypes.findByCodeIgnoreCase(request.code()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new InvalidAdminRequest("Mã loại phòng " + request.code() + " đã tồn tại.");
            }
        });
        apply(type, request);
        roomTypes.save(type);
        return toView(type, rooms.countByRoomTypeId(id));
    }

    /**
     * Xoá cứng, CHỈ khi không còn gì trỏ tới.
     *
     * <p>Đơn cũ đã chụp lại tên và giá loại phòng, nhưng vẫn giữ khoá ngoại tới
     * đây để tra cứu. Xoá loại phòng còn đơn là hoặc lỗi khoá ngoại khó hiểu,
     * hoặc mất lịch sử. Lối đi đúng là tắt {@code active} — loại phòng biến mất
     * khỏi trang đặt phòng mà đơn cũ vẫn nguyên.
     */
    @Transactional
    public void delete(Long id) {
        RoomType type = require(id);
        long bookingCount = bookings.countByRoomTypeId(id);
        if (bookingCount > 0) {
            throw new ResourceInUse(
                    "Loại phòng này đang có " + bookingCount + " đơn đặt phòng nên không xoá được. "
                            + "Hãy tắt trạng thái hoạt động để ẩn nó khỏi trang đặt phòng.");
        }
        long roomCount = rooms.countByRoomTypeId(id);
        if (roomCount > 0) {
            throw new ResourceInUse(
                    "Còn " + roomCount + " phòng vật lý thuộc loại này. Xoá hoặc chuyển các phòng đó trước.");
        }
        images.findByRoomTypeIdOrderByDisplayOrderAscIdAsc(id)
                .forEach(image -> storage.delete(image.getUrl(), image.getPublicId()));
        roomTypes.delete(type);
    }

    @Transactional
    public RoomTypeView setAmenities(Long id, List<Long> amenityIds) {
        RoomType type = require(id);
        List<Amenity> selected = amenities.findAllById(amenityIds);
        if (selected.size() != amenityIds.stream().distinct().count()) {
            throw new InvalidAdminRequest("Có tiện ích không tồn tại trong danh sách đã chọn.");
        }
        type.setAmenities(new LinkedHashSet<>(selected));
        roomTypes.save(type);
        return toView(type, rooms.countByRoomTypeId(id));
    }

    @Transactional
    public RoomTypeView addImage(Long id, AddImageRequest request) {
        RoomType type = require(id);
        List<RoomTypeImage> existing = images.findByRoomTypeIdOrderByDisplayOrderAscIdAsc(id);

        RoomTypeImage image = new RoomTypeImage();
        image.setRoomType(type);
        image.setUrl(request.url());
        image.setPublicId(request.publicId());
        image.setAltText(request.altText());
        image.setDisplayOrder(existing.size());
        // Ảnh đầu tiên tự thành ảnh bìa: một loại phòng không có ảnh bìa sẽ hiện
        // ra ô trống trên trang đặt phòng, và không ai nhớ bấm đặt bìa thủ công.
        image.setCover(existing.isEmpty());
        images.save(image);
        return toView(type, rooms.countByRoomTypeId(id));
    }

    @Transactional
    public RoomTypeView reorderImages(Long id, List<ImageOrderEntry> entries) {
        RoomType type = require(id);
        Map<Long, RoomTypeImage> byId = images.findByRoomTypeIdOrderByDisplayOrderAscIdAsc(id).stream()
                .collect(Collectors.toMap(RoomTypeImage::getId, Function.identity()));

        long coverCount = entries.stream().filter(ImageOrderEntry::cover).count();
        if (coverCount > 1) {
            throw new InvalidAdminRequest("Mỗi loại phòng chỉ có một ảnh bìa.");
        }
        for (ImageOrderEntry entry : entries) {
            RoomTypeImage image = byId.get(entry.id());
            if (image == null) {
                throw new InvalidAdminRequest("Ảnh #" + entry.id() + " không thuộc loại phòng này.");
            }
            image.setDisplayOrder(entry.displayOrder());
            image.setCover(entry.cover());
            images.save(image);
        }
        return toView(type, rooms.countByRoomTypeId(id));
    }

    @Transactional
    public void deleteImage(Long imageId) {
        RoomTypeImage image = images.findById(imageId)
                .orElseThrow(() -> new AdminResourceNotFound("ảnh #" + imageId));
        // Xoá tệp trước, bản ghi sau. Ngược lại thì một lỗi ở bước xoá bản ghi
        // để lại một dòng trỏ tới tệp không còn tồn tại.
        storage.delete(image.getUrl(), image.getPublicId());
        images.delete(image);
    }

    // ─────────────────────────────────────────────────────────────────────

    private RoomType require(Long id) {
        return roomTypes.findWithAmenities(id)
                .orElseThrow(() -> new AdminResourceNotFound("loại phòng #" + id));
    }

    private static void apply(RoomType type, RoomTypeRequest request) {
        type.setCode(request.code().trim());
        type.setSlug(request.slug().trim().toLowerCase());
        type.setName(request.name().trim());
        type.setShortDescription(request.shortDescription());
        type.setDescription(request.description());
        type.setBasePrice(request.basePrice());
        type.setCapacityAdults(request.capacityAdults());
        type.setCapacityChildren(request.capacityChildren());
        type.setBedInfo(request.bedInfo());
        type.setAreaSqm(request.areaSqm());
        type.setDisplayOrder(request.displayOrder());
        type.setActive(request.active());
    }

    private RoomTypeView toView(RoomType type, long roomCount) {
        return new RoomTypeView(
                type.getId(),
                type.getCode(),
                type.getSlug(),
                type.getName(),
                type.getShortDescription(),
                type.getDescription(),
                type.getBasePrice(),
                type.getCapacityAdults(),
                type.getCapacityChildren(),
                type.getBedInfo(),
                type.getAreaSqm(),
                type.getDisplayOrder(),
                type.isActive(),
                roomCount,
                type.getAmenities().stream().map(AdminRoomTypeService::toAmenityView).toList(),
                images.findByRoomTypeIdOrderByDisplayOrderAscIdAsc(type.getId()).stream()
                        .map(image -> new RoomTypeImageView(
                                image.getId(),
                                image.getUrl(),
                                image.getPublicId(),
                                image.getAltText(),
                                image.getDisplayOrder(),
                                image.isCover()))
                        .toList());
    }

    private static AmenityView toAmenityView(Amenity amenity) {
        return new AmenityView(
                amenity.getId(),
                amenity.getCode(),
                amenity.getName(),
                amenity.getIcon(),
                amenity.getCategory().name());
    }
}
