package com.tvh.homestay.admin;

import com.tvh.homestay.admin.dto.AdminDtos.BookingDetail;
import com.tvh.homestay.admin.dto.AdminDtos.BookingRow;
import com.tvh.homestay.admin.dto.AdminDtos.NoteRequest;
import com.tvh.homestay.admin.dto.AdminDtos.OutboundEmailView;
import com.tvh.homestay.admin.dto.AdminDtos.ResendEmailRequest;
import com.tvh.homestay.admin.dto.AdminDtos.TransitionRequest;
import com.tvh.homestay.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xử lý đơn ở khu quản trị.
 *
 * <p>Phân quyền do {@code SecurityConfig} lo bằng một dòng
 * {@code /api/admin/** → hasRole("ADMIN")}; không lớp nào ở đây tự kiểm quyền
 * lần nữa, vì hai chỗ kiểm là hai chỗ để lệch nhau.
 */
@RestController
public class AdminBookingController {

    /** Trần số dòng một trang. Client gửi size=100000 thì vẫn chỉ nhận được chừng này. */
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminBookingService bookings;

    public AdminBookingController(AdminBookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/api/admin/bookings")
    public Page<BookingRow> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return bookings.search(status, from, to, q,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                        Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/api/admin/bookings/{id}")
    public BookingDetail detail(@PathVariable Long id) {
        return bookings.detail(id);
    }

    @PostMapping("/api/admin/bookings/{id}/transition")
    public BookingDetail transition(
            @PathVariable Long id,
            @Valid @RequestBody TransitionRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return bookings.transition(
                id, request.toStatus(), request.note(), CurrentAdmin.idOf(principal));
    }

    @PostMapping("/api/admin/bookings/{id}/note")
    public BookingDetail addNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return bookings.addNote(id, request.content(), CurrentAdmin.idOf(principal));
    }

    @PostMapping("/api/admin/bookings/{id}/resend-email")
    public OutboundEmailView resendEmail(
            @PathVariable Long id,
            @RequestBody(required = false) ResendEmailRequest request) {
        return bookings.resendEmail(id, request == null ? null : request.template());
    }
}
