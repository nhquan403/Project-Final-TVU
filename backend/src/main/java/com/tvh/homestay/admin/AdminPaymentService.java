package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.ReconcileRow;
import com.tvh.homestay.admin.exception.AdminExceptions.AdminResourceNotFound;
import com.tvh.homestay.admin.exception.AdminExceptions.InvalidAdminRequest;
import com.tvh.homestay.booking.BookingStateMachine;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingNote;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.entity.PaymentStatus;
import com.tvh.homestay.booking.repository.BookingNoteRepository;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.mail.EmailOutboxService;
import com.tvh.homestay.mail.MailService;
import com.tvh.homestay.payment.entity.Payment;
import com.tvh.homestay.payment.entity.PaymentAttemptStatus;
import com.tvh.homestay.payment.entity.ReconcileStatus;
import com.tvh.homestay.payment.repository.PaymentRepository;
import com.tvh.homestay.payment.repository.PaymentWebhookEventRepository;
import com.tvh.homestay.user.entity.User;
import com.tvh.homestay.user.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hàng đợi đối soát — nơi những khoản tiền không tự khớp được có người xử lý.
 *
 * <p>Màn hình này là điều kiện để ba nhánh của Phase 6 (thiếu tiền, thừa tiền,
 * tiền về muộn không gán lại được phòng) không kết thúc bằng "tiền nằm im trong
 * bảng và không ai biết". Nó không phải màn hình tuỳ chọn.
 */
@Service
public class AdminPaymentService {

    private final PaymentRepository payments;
    private final PaymentWebhookEventRepository events;
    private final BookingRepository bookings;
    private final BookingNoteRepository notes;
    private final BookingStateMachine stateMachine;
    private final EmailOutboxService outbox;
    private final Clock clock;
    private final UserRepository users;

    public AdminPaymentService(
            PaymentRepository payments,
            PaymentWebhookEventRepository events,
            BookingRepository bookings,
            BookingNoteRepository notes,
            BookingStateMachine stateMachine,
            EmailOutboxService outbox,
            Clock clock,
            UserRepository users) {
        this.payments = payments;
        this.events = events;
        this.bookings = bookings;
        this.notes = notes;
        this.stateMachine = stateMachine;
        this.outbox = outbox;
        this.clock = clock;
        this.users = users;
    }

    /**
     * @param reconcileStatus lọc theo một trạng thái cụ thể; để trống thì chỉ
     *     trả về những khoản CHƯA xử lý — màn hình mặc định phải là danh sách
     *     việc cần làm, không phải kho lưu trữ.
     */
    @Transactional(readOnly = true)
    public Page<ReconcileRow> queue(String reconcileStatus, Pageable pageable) {
        return payments.findReconcileQueue(parseReconcile(reconcileStatus), pageable)
                .map(this::toRow);
    }

    /**
     * Số dòng CÒN cần đối soát — con số hiện trên dashboard và trên sidebar.
     *
     * <p>Chỉ đếm {@code NEEDS_REVIEW} và {@code REFUND_REQUIRED}. Khoản đã đánh
     * dấu {@code RESOLVED} vẫn ở lại trong dữ liệu để tra cứu, nhưng không còn
     * là việc phải làm.
     */
    @Transactional(readOnly = true)
    public long pendingCount() {
        return payments.countByReconcileStatusIn(
                java.util.List.of(ReconcileStatus.NEEDS_REVIEW, ReconcileStatus.REFUND_REQUIRED));
    }

    /**
     * Đánh dấu đã xử lý xong.
     *
     * <p>Ghi luôn một dòng {@code booking_notes}: "đã đối soát" mà không nói ai
     * đối soát và kết luận gì thì lần sau có tranh chấp không ai dựng lại được
     * quyết định đó.
     */
    @Transactional
    public ReconcileRow resolve(Long paymentId, String note, Long adminId) {
        Payment payment = require(paymentId);
        payment.setReconcileStatus(ReconcileStatus.RESOLVED);
        payments.save(payment);
        recordNote(payment, admin(adminId),
                "Đối soát: " + (note == null || note.isBlank() ? "đã xử lý" : note.trim()));
        return toRow(payment);
    }

    /**
     * Xác nhận đơn BẰNG TAY sau khi người thật đã kiểm tra sao kê.
     *
     * <p>Vẫn đi qua máy trạng thái với {@code actor = ADMIN}: đây chính là
     * trường hợp mà dòng nhật ký "ai xác nhận, lúc nào, vì sao" đáng giá nhất.
     */
    @Transactional
    public ReconcileRow confirmManually(Long paymentId, String note, Long adminId) {
        Payment payment = require(paymentId);
        Booking booking = bookings.findById(payment.getBooking().getId())
                .orElseThrow(() -> new AdminResourceNotFound("đơn của lần thanh toán này"));

        if (!BookingStateMachine.canTransition(booking.getStatus(), BookingStatus.CONFIRMED)) {
            throw new InvalidAdminRequest(
                    "Đơn đang ở trạng thái " + booking.getStatus()
                            + " nên không xác nhận trực tiếp được. Đưa đơn về chờ đối soát trước.");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        payment.setStatus(PaymentAttemptStatus.SUCCEEDED);
        payment.setReconcileStatus(ReconcileStatus.RESOLVED);
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(now);
        }
        payments.save(payment);

        String reason = note == null || note.isBlank()
                ? "Quản trị viên xác nhận thủ công sau khi đối soát sao kê"
                : note.trim();
        stateMachine.transition(
                booking, BookingStatus.CONFIRMED, HistoryActor.ADMIN, admin(adminId), reason);
        booking.setPaymentStatus(
                payment.getAmountReceived().compareTo(booking.getTotalAmount()) >= 0
                        ? PaymentStatus.PAID
                        : PaymentStatus.DEPOSIT_PAID);
        bookings.save(booking);
        outbox.enqueue(booking, MailService.TEMPLATE_CONFIRMED, Map.of());
        recordNote(payment, admin(adminId), reason);
        return toRow(payment);
    }

    // ─────────────────────────────────────────────────────────────────────

    /** Nạp trong CHÍNH transaction đang chạy — xem javadoc của {@link CurrentAdmin}. */
    private User admin(Long adminId) {
        return adminId == null ? null : users.findById(adminId).orElse(null);
    }

    private Payment require(Long id) {
        return payments.findById(id)
                .orElseThrow(() -> new AdminResourceNotFound("lần thanh toán #" + id));
    }

    private void recordNote(Payment payment, User admin, String content) {
        BookingNote note = new BookingNote();
        note.setBooking(payment.getBooking());
        note.setAuthor(admin);
        note.setContent(content);
        notes.save(note);
    }

    private static ReconcileStatus parseReconcile(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ReconcileStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidAdminRequest("Trạng thái đối soát không hợp lệ: " + status);
        }
    }

    private ReconcileRow toRow(Payment payment) {
        Booking booking = payment.getBooking();
        List<String> payloads = events.findByPaymentIdOrderByIdAsc(payment.getId()).stream()
                .map(event -> event.getPayload())
                .toList();
        return new ReconcileRow(
                payment.getId(),
                booking.getCode(),
                booking.getGuestName(),
                payment.getTransferContent(),
                payment.getAmountExpected(),
                payment.getAmountReceived(),
                payment.getStatus().name(),
                payment.getReconcileStatus().name(),
                booking.getStatus().name(),
                payment.getPaidAt(),
                payloads);
    }
}
