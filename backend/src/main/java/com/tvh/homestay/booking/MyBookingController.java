package com.tvh.homestay.booking;

import com.tvh.homestay.auth.AuthenticatedUser;
import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đơn của chính người đang đăng nhập.
 *
 * <p>Lọc theo id lấy từ TOKEN. Không bao giờ nhận {@code userId} từ client —
 * đó là cách nhanh nhất để một khách xem được đơn của khách khác.
 */
@RestController
public class MyBookingController {

    private static final int MAX_PAGE_SIZE = 50;

    private final BookingService bookings;

    public MyBookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/api/me/bookings")
    public Page<BookingResponse> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return bookings.listForUser(
                principal.id(), PageRequest.of(Math.max(0, page), Math.min(size, MAX_PAGE_SIZE)));
    }

    @GetMapping("/api/me/bookings/{code}")
    public BookingResponse detail(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable String code) {
        return bookings.getForUser(code, principal.id());
    }
}
