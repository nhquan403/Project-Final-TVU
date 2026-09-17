package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.AffectedBooking;
import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureResult;
import com.tvh.homestay.admin.dto.AdminDtos.RoomClosureView;
import com.tvh.homestay.admin.dto.AdminDtos.RoomRequest;
import com.tvh.homestay.admin.dto.AdminDtos.RoomStatusResult;
import com.tvh.homestay.admin.dto.AdminDtos.RoomView;
import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.ClosureOverlap;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.admin.exception.AdminExceptions.ResourceInUse;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.booking.repository.BookingRoomRepository;
import com.tvh.homestay.common.SqlStates;
import com.tvh.homestay.room.entity.Room;
import com.tvh.homestay.room.entity.RoomClosure;
import com.tvh.homestay.room.entity.RoomStatus;
import com.tvh.homestay.room.entity.RoomType;
import com.tvh.homestay.room.repository.RoomClosureRepository;
import com.tvh.homestay.room.repository.RoomRepository;
import com.tvh.homestay.room.repository.RoomTypeRepository;
import com.tvh.homestay.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD phòng vật lý và việc đưa phòng ra/vào vận hành. */
@Service
public class AdminRoomService {

    private final RoomRepository rooms;
    private final RoomTypeRepository roomTypes;
    private final BookingRepository bookings;
    private final BookingRoomRepository bookingRooms;
    private final RoomClosureRepository closures;
    private final UserRepository users;
    private final Clock clock;

    public AdminRoomService(
            RoomRepository rooms,
            RoomTypeRepository roomTypes,
            BookingRepository bookings,
            BookingRoomRepository bookingRooms,
            RoomClosureRepository closures,
            UserRepository users,
            Clock clock) {
        this.rooms = rooms;
        this.roomTypes = roomTypes;
        this.bookings = bookings;
        this.bookingRooms = bookingRooms;
        this.closures = closures;
        this.users = users;
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
                        .map(AdminRoomService::toAffected)
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

    // ─── Khoảng ngày không nhận khách ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RoomClosureView> listClosures(Long roomId) {
        require(roomId);
        return closures.findByRoomIdOrderByFromDateAsc(roomId).stream()
                .map(AdminRoomService::toView)
                .toList();
    }

    /**
     * Đóng một phòng trong một khoảng ngày.
     *
     * <p><b>Không huỷ đơn nào</b>, y như {@link #changeStatus}. Đơn đang nằm
     * trong khoảng vừa đóng được LIỆT KÊ để người quyết định nhìn thấy: đổi
     * phòng cho khách hay dời lịch sửa chữa là quyết định kinh doanh.
     *
     * <p>Cố tình KHÔNG chặn việc đóng một phòng đang có đơn. Chặn thì chủ
     * homestay không ghi nhận được sự thật "phòng này hỏng từ ngày mai" chỉ vì
     * hệ thống còn một đơn cũ — và sự thật đó vẫn xảy ra dù hệ thống có cho ghi
     * hay không.
     */
    @Transactional
    public RoomClosureResult addClosure(Long roomId, RoomClosureRequest request, Long adminId) {
        Room room = require(roomId);
        if (!request.toDate().isAfter(request.fromDate())) {
            throw new InvalidAdminRequest(
                    "Ngày mở bán lại phải sau ngày bắt đầu đóng. Đóng một đêm duy nhất thì "
                            + "chọn ngày mở bán lại là hôm sau.");
        }

        RoomClosure closure = new RoomClosure();
        closure.setRoom(room);
        closure.setFromDate(request.fromDate());
        closure.setToDate(request.toDate());
        closure.setReason(request.reason());
        closure.setCreatedBy(adminId == null ? null : users.findById(adminId).orElse(null));

        try {
            // Ghi xuống NGAY để ràng buộc chống chồng lấn nổ ở đây, chỗ còn dịch
            // được lỗi thành câu tiếng Việt. Để tới lúc commit thì ngoại lệ bật
            // ra ngoài mọi handler nghiệp vụ và client nhận 500.
            closures.saveAndFlush(closure);
        } catch (DataIntegrityViolationException e) {
            if (SqlStates.isExclusionViolation(e)) {
                throw new ClosureOverlap(
                        "Phòng " + room.getRoomNumber() + " đã có một khoảng đóng chồng lên "
                                + "khoảng này. Hãy xoá hoặc sửa khoảng cũ trước.");
            }
            throw e;
        }
        return new RoomClosureResult(toView(closure), affectedBy(roomId, request.fromDate(), request.toDate()));
    }

    @Transactional
    public void removeClosure(Long closureId) {
        RoomClosure closure = closures.findById(closureId)
                .orElseThrow(() -> new AdminResourceNotFound("khoảng đóng phòng #" + closureId));
        closures.delete(closure);
    }

    /**
     * Đơn còn hiệu lực GIAO NHAU với khoảng vừa đóng.
     *
     * <p>Lọc theo khoảng chứ không lấy mọi đơn tương lai của phòng: một đơn ba
     * tháng nữa không liên quan gì tới việc sơn phòng tuần sau, và liệt kê nó ra
     * chỉ làm cảnh báo mất trọng lượng.
     */
    private List<AffectedBooking> affectedBy(Long roomId, LocalDate from, LocalDate to) {
        return bookings.findActiveFutureByRoom(roomId, LocalDate.now(clock)).stream()
                .filter(booking -> overlaps(booking, from, to))
                .map(AdminRoomService::toAffected)
                .toList();
    }

    /** Giao nhau theo quy ước nửa mở {@code [)} — đúng quy ước của cột {@code blocked}. */
    private static boolean overlaps(Booking booking, LocalDate from, LocalDate to) {
        return booking.getCheckIn().isBefore(to) && booking.getCheckOut().isAfter(from);
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

    private static AffectedBooking toAffected(Booking booking) {
        return new AffectedBooking(
                booking.getCode(),
                booking.getGuestName(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getStatus().name());
    }

    private static RoomClosureView toView(RoomClosure closure) {
        return new RoomClosureView(
                closure.getId(),
                closure.getRoom().getId(),
                closure.getRoom().getRoomNumber(),
                closure.getFromDate(),
                closure.getToDate(),
                (int) ChronoUnit.DAYS.between(closure.getFromDate(), closure.getToDate()),
                closure.getReason());
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
