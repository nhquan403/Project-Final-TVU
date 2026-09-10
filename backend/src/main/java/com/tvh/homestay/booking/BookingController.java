package com.tvh.homestay.booking;

import com.tvh.homestay.auth.AuthenticatedUser;
import com.tvh.homestay.booking.dto.BookingDtos.BookingResponse;
import com.tvh.homestay.booking.dto.BookingDtos.CancelRequest;
import com.tvh.homestay.booking.dto.BookingDtos.CreateBookingRequest;
import com.tvh.homestay.booking.dto.BookingDtos.LookupRequest;
import com.tvh.homestay.booking.dto.BookingDtos.PaymentStatusResponse;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.user.entity.User;
import com.tvh.homestay.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Các thao tác đặt phòng công khai.
 *
 * <p>Công khai ở tầng phân quyền, nhưng KHÔNG có nghĩa là ai cũng làm gì cũng
 * được: quyền trên từng đơn được kiểm ở tầng service bằng {@code accessToken}
 * hoặc số điện thoại. Đó là cách duy nhất phục vụ được khách vãng lai không có
 * tài khoản.
 */
@RestController
public class BookingController {

    private final BookingService bookings;
    private final BookingStateMachine stateMachine;
    private final UserRepository users;

    public BookingController(
            BookingService bookings, BookingStateMachine stateMachine, UserRepository users) {
        this.bookings = bookings;
        this.stateMachine = stateMachine;
        this.users = users;
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest httpRequest) {

        // Khách đã đăng nhập thì gắn đơn vào tài khoản; khách vãng lai thì không.
        // Danh tính lấy từ token, KHÔNG bao giờ từ thân request.
        User user = principal == null ? null : users.findById(principal.id()).orElse(null);

        return bookings.create(
                request,
                user,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/api/bookings/lookup")
    public BookingResponse lookup(@Valid @RequestBody LookupRequest request) {
        return bookings.lookup(request.code(), request.phone());
    }

    @PostMapping("/api/bookings/{code}/cancel")
    public BookingResponse cancel(
            @PathVariable String code,
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(name = "X-Booking-Token", required = false) String headerToken,
            @Valid @RequestBody(required = false) CancelRequest request) {

        String accessToken = token != null ? token : headerToken;
        String phone = request == null ? null : request.phone();
        Booking booking = bookings.requireByCodeAndTokenOrPhone(code, accessToken, phone);

        String reason = request == null || request.reason() == null
                ? "Khách tự huỷ"
                : request.reason();
        Booking cancelled = stateMachine.transition(
                booking, BookingStatus.CANCELLED, HistoryActor.GUEST, reason);
        return bookings.toResponse(cancelled, false);
    }

    /**
     * Màn hình QR hỏi endpoint này liên tục cho tới khi tiền về.
     *
     * <p>Bắt buộc kèm {@code token}: mã đơn nằm trên sao kê ngân hàng nên nếu
     * chỉ cần mã là xem được trạng thái thì bất kỳ ai thấy sao kê cũng theo dõi
     * được đơn của người khác.
     */
    @GetMapping("/api/bookings/{code}/payment-status")
    public PaymentStatusResponse paymentStatus(
            @PathVariable String code,
            @RequestParam(name = "token", required = false) String token) {
        return bookings.paymentStatus(code, token);
    }
}
