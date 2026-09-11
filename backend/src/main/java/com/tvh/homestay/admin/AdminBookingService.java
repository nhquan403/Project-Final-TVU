package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.AssignedRoomView;
import com.tvh.homestay.admin.dto.AdminDtos.BookingDetail;
import com.tvh.homestay.admin.dto.AdminDtos.BookingRow;
import com.tvh.homestay.admin.dto.AdminDtos.NoteView;
import com.tvh.homestay.admin.dto.AdminDtos.OutboundEmailView;
import com.tvh.homestay.admin.dto.AdminDtos.PaymentAttemptView;
import com.tvh.homestay.admin.dto.AdminDtos.StatusHistoryEntry;
import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.availability.AvailabilityService;
import com.tvh.homestay.booking.BookingStateMachine;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingNote;
import com.tvh.homestay.booking.entity.BookingRoom;
import com.tvh.homestay.booking.entity.BookingRoomStatus;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.repository.BookingNoteRepository;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.booking.repository.BookingRoomRepository;
import com.tvh.homestay.booking.repository.BookingStatusHistoryRepository;
import com.tvh.homestay.mail.EmailOutboxService;
import com.tvh.homestay.payment.entity.OutboundEmail;
import com.tvh.homestay.payment.repository.OutboundEmailRepository;
import com.tvh.homestay.payment.repository.PaymentRepository;
import com.tvh.homestay.user.entity.User;
import com.tvh.homestay.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Xử lý đơn ở khu quản trị: lọc, xem chi tiết, đổi trạng thái, ghi chú, gửi lại thư. */
@Service
public class AdminBookingService {

    private final BookingRepository bookings;
    private final BookingRoomRepository bookingRooms;
    private final BookingStatusHistoryRepository history;
    private final BookingNoteRepository notes;
    private final PaymentRepository payments;
    private final OutboundEmailRepository outbox;
    private final EmailOutboxService outboxService;
    private final BookingStateMachine stateMachine;
    private final UserRepository users;

    public AdminBookingService(
            BookingRepository bookings,
            BookingRoomRepository bookingRooms,
            BookingStatusHistoryRepository history,
            BookingNoteRepository notes,
            PaymentRepository payments,
            OutboundEmailRepository outbox,
            EmailOutboxService outboxService,
            BookingStateMachine stateMachine,
            UserRepository users) {
        this.bookings = bookings;
        this.bookingRooms = bookingRooms;
        this.history = history;
        this.notes = notes;
        this.payments = payments;
        this.outbox = outbox;
        this.outboxService = outboxService;
        this.stateMachine = stateMachine;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<BookingRow> search(
            String status, LocalDate from, LocalDate to, String q, Pageable pageable) {
        return bookings
                .search(parseStatus(status), from, to, likePattern(q), pageable)
                .map(AdminBookingService::toRow);
    }

    @Transactional(readOnly = true)
    public BookingDetail detail(Long id) {
        return toDetail(require(id));
    }

    /**
     * Đổi trạng thái đơn.
     *
     * <p>Đi qua {@link BookingStateMachine} như mọi nơi khác, kèm
     * {@code actor = ADMIN} và người thực hiện. Chuyển sai luồng ném
     * {@code InvalidStateTransition} và {@code ApiExceptionHandler} đã đổi nó
     * thành 409 kèm mã ổn định — không map lại ở đây.
     */
    @Transactional
    public BookingDetail transition(Long id, String toStatus, String note, Long adminId) {
        Booking booking = require(id);
        BookingStatus target = parseStatus(toStatus);
        if (target == null) {
            throw new InvalidAdminRequest("Trạng thái không hợp lệ: " + toStatus);
        }
        String reason = note == null || note.isBlank() ? "Quản trị viên đổi trạng thái" : note;
        stateMachine.transition(booking, target, HistoryActor.ADMIN, admin(adminId), reason);
        return toDetail(booking);
    }

    @Transactional
    public BookingDetail addNote(Long id, String content, Long adminId) {
        Booking booking = require(id);
        BookingNote note = new BookingNote();
        note.setBooking(booking);
        note.setAuthor(admin(adminId));
        note.setContent(content.trim());
        notes.save(note);
        return toDetail(booking);
    }

    /**
     * Gửi lại một lá thư đã xếp hàng trước đó.
     *
     * <p>Xếp hàng một dòng outbox MỚI chứ không gọi thẳng {@code MailService}:
     * gọi thẳng là bỏ qua hộp thư đi, và câu hỏi "đã gửi lại chưa" lại quay về
     * chỗ không trả lời được bằng SQL.
     */
    @Transactional
    public OutboundEmailView resendEmail(Long id, String template) {
        Booking booking = require(id);
        List<OutboundEmail> sent = outbox.findByBookingIdOrderByIdAsc(booking.getId());
        OutboundEmail original = sent.stream()
                .filter(email -> template == null || template.isBlank()
                        || template.equals(email.getTemplate()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new InvalidAdminRequest(
                        "Đơn này chưa có lá thư nào để gửi lại."));
        return toEmailView(outboxService.requeue(original));
    }

    // ─────────────────────────────────────────────────────────────────────

    /** Nạp trong CHÍNH transaction đang chạy — xem javadoc của {@link CurrentAdmin}. */
    private User admin(Long adminId) {
        return adminId == null ? null : users.findById(adminId).orElse(null);
    }

    private Booking require(Long id) {
        return bookings.findById(id)
                .orElseThrow(() -> new AdminResourceNotFound("đơn đặt phòng #" + id));
    }

    private static BookingStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return BookingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidAdminRequest("Trạng thái không hợp lệ: " + status);
        }
    }

    /** Chuẩn bị sẵn mẫu LIKE ở đây để truy vấn không phải nối chuỗi. */
    private static String likePattern(String q) {
        return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static BookingRow toRow(Booking booking) {
        return new BookingRow(
                booking.getId(),
                booking.getCode(),
                booking.getGuestName(),
                booking.getGuestPhone(),
                booking.getRoomTypeNameSnapshot(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getRoomQuantity(),
                booking.getTotalAmount(),
                booking.getDepositAmount(),
                booking.getStatus().name(),
                booking.getPaymentStatus().name(),
                booking.getCreatedAt());
    }

    private BookingDetail toDetail(Booking booking) {
        List<AssignedRoomView> rooms = bookingRooms
                .findWithRoomByBookingIdAndStatus(booking.getId(), BookingRoomStatus.ACTIVE)
                .stream()
                .map(AdminBookingService::toRoomView)
                .toList();

        List<StatusHistoryEntry> timeline = history
                .findWithActorByBookingId(booking.getId())
                .stream()
                .map(entry -> new StatusHistoryEntry(
                        entry.getFromStatus() == null ? null : entry.getFromStatus().name(),
                        entry.getToStatus().name(),
                        entry.getActor().name(),
                        entry.getChangedBy() == null ? null : entry.getChangedBy().getFullName(),
                        entry.getNote(),
                        entry.getCreatedAt()))
                .toList();

        List<PaymentAttemptView> attempts = payments
                .findByBookingIdOrderByAttemptNoAsc(booking.getId())
                .stream()
                .map(payment -> new PaymentAttemptView(
                        payment.getId(),
                        payment.getAttemptNo(),
                        payment.getTransferContent(),
                        payment.getAmountExpected(),
                        payment.getAmountReceived(),
                        payment.getStatus().name(),
                        payment.getReconcileStatus().name(),
                        payment.getPaidAt(),
                        payment.getExpiresAt()))
                .toList();

        List<OutboundEmailView> emails = outbox
                .findByBookingIdOrderByIdAsc(booking.getId())
                .stream()
                .map(AdminBookingService::toEmailView)
                .toList();

        List<NoteView> noteViews = notes
                .findWithAuthorByBookingId(booking.getId())
                .stream()
                .map(note -> new NoteView(
                        note.getId(),
                        note.getAuthor() == null ? "(tài khoản đã xoá)" : note.getAuthor().getFullName(),
                        note.getContent(),
                        note.getCreatedAt()))
                .toList();

        return new BookingDetail(
                booking.getId(),
                booking.getCode(),
                booking.getStatus().name(),
                booking.getPaymentStatus().name(),
                booking.getGuestName(),
                booking.getGuestEmail(),
                booking.getGuestPhone(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                AvailabilityService.nights(booking.getCheckIn(), booking.getCheckOut()),
                booking.getRoomQuantity(),
                booking.getAdults(),
                booking.getChildren(),
                booking.getRoomTypeNameSnapshot(),
                booking.getUnitPriceSnapshot(),
                booking.getSubtotalAmount(),
                booking.getDiscountAmount(),
                booking.getTotalAmount(),
                booking.getDepositAmount(),
                booking.getPromotion() == null ? null : booking.getPromotion().getCode(),
                booking.getSpecialRequest(),
                booking.getHoldExpiresAt(),
                booking.getCreatedAt(),
                rooms,
                timeline,
                attempts,
                emails,
                noteViews);
    }

    private static AssignedRoomView toRoomView(BookingRoom bookingRoom) {
        return new AssignedRoomView(
                bookingRoom.getRoom().getId(),
                bookingRoom.getRoom().getRoomNumber(),
                bookingRoom.getRoom().getFloor(),
                bookingRoom.getRoom().getStatus().name());
    }

    private static OutboundEmailView toEmailView(OutboundEmail email) {
        return new OutboundEmailView(
                email.getId(),
                email.getTemplate(),
                email.getToEmail(),
                email.getStatus().name(),
                email.getAttempts(),
                email.getLastError(),
                email.getSentAt(),
                email.getCreatedAt());
    }
}
