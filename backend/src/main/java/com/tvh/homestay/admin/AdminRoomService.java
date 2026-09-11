package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.AffectedBooking;
import com.tvh.homestay.admin.dto.AdminDtos.RoomRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomStatusResult;
import com.tvh.homestay.admin.dto.AdminDtos.RoomView;
import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.admin.exception.AdminExceptions.ResourceInUse;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.booking.repository.BookingRoomRepository;
import com.tvh.homestay.room.entity.Room;
import com.tvh.homestay.room.entity.RoomStatus;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.repository.RoomRepository;
import com.tvh.homestay.room.repository.RoomTypeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD phòng vật lý và việc đưa phòng ra/vào vận hành. */
@Service
public class AdminRoomService {

    private final RoomRepository rooms;
    private final RoomTypeRepository roomTypes;
    private final BookingRepository bookings;
    private final BookingRoomRepository bookingRooms;
    private final Clock clock;

    public AdminRoomService(
            RoomRepository rooms,
            RoomTypeRepository roomTypes,
            BookingRepository bookings,
            BookingRoomRepository bookingRooms,
            Clock clock) {
        this.rooms = rooms;
        this.roomTypes = roomTypes;
        this.bookings = bookings;
        this.bookingRooms = bookingRooms;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RoomView> list(Long roomTypeId) {
        List<Room> found = roomTypeId == null
                ? rooms.findAllByOrderByRoomNumberAsc()
                : rooms.findByRoomTypeIdOrderByRoomNumberAsc(roomTypeId);
        return found.stream().map(AdminRoomService::toView).toList();
    }

    @Transactional
    public RoomView create(RoomRequest request) {
        if (rooms.existsByRoomNumberIgnoreCase(request.roomNumber().trim())) {
            throw new InvalidAdminRequest("Số phòng " + request.roomNumber() + " đã tồn tại.");
        }
        Room room = new Room();
        apply(room, request);
        rooms.save(room);
        return toView(room);
    }

    @Transactional
    public RoomView update(Long id, RoomRequest request) {
        Room room = require(id);
        apply(room, request);
        rooms.save(room);
        return toView(room);
    }

    /**
     * Đổi trạng thái vận hành của phòng.
     *
     * <p><b>Không tự huỷ đơn nào.</b> Đưa phòng đi bảo trì trong khi nó còn đơn
     * tương lai là một quyết định kinh doanh: có thể đổi phòng cho khách, có thể
     * hoãn bảo trì. Việc của hệ thống là LIỆT KÊ đúng những đơn bị ảnh hưởng để
     * người quyết định nhìn thấy chúng, không phải tự quyết thay.
     */
    @Transactional
    public RoomStatusResult changeStatus(Long id, String status) {
        Room room = require(id);
        RoomStatus target = parseStatus(status);
        room.setStatus(target);
        rooms.save(room);

        List<AffectedBooking> affected = target == RoomStatus.AVAILABLE
                ? List.of()
                : bookings.findActiveFutureByRoom(id, LocalDate.now(clock)).stream()
                        .map(booking -> new AffectedBooking(
                                booking.getCode(),
                                booking.getGuestName(),
                                booking.getCheckIn(),
                                booking.getCheckOut(),
                                booking.getStatus().name()))
                        .toList();
        return new RoomStatusResult(toView(room), affected);
    }

    @Transactional
    public void delete(Long id) {
        Room room = require(id);
        long assignments = bookingRooms.countByRoomId(id);
        if (assignments > 0) {
            throw new ResourceInUse(
                    "Phòng này đã từng được gán cho " + assignments + " đơn nên không xoá được. "
                            + "Hãy chuyển trạng thái sang NGỪNG KHAI THÁC để ngừng bán nó.");
        }
        rooms.delete(room);
    }

    // ─────────────────────────────────────────────────────────────────────

    private Room require(Long id) {
        return rooms.findById(id).orElseThrow(() -> new AdminResourceNotFound("phòng #" + id));
    }

    private void apply(Room room, RoomRequest request) {
        RoomType type = roomTypes.findById(request.roomTypeId())
                .orElseThrow(() -> new InvalidAdminRequest("Loại phòng không tồn tại."));
        room.setRoomType(type);
        room.setRoomNumber(request.roomNumber().trim());
        room.setFloor(request.floor());
        room.setNote(request.note());
    }

    private static RoomStatus parseStatus(String status) {
        try {
            return RoomStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new InvalidAdminRequest("Trạng thái phòng không hợp lệ: " + status);
        }
    }

    private static RoomView toView(Room room) {
        return new RoomView(
                room.getId(),
                room.getRoomType().getId(),
                room.getRoomType().getName(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus().name(),
                room.getNote());
    }
}
