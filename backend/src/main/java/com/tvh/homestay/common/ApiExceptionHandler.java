package com.tvh.homestay.common;

import com.tvh.homestay.admin.exception.AdminExceptions.AdminException;
import com.tvh.homestay.booking.exception.BookingExceptions.BookingException;
import com.tvh.homestay.booking.exception.BookingExceptions.BookingNotFound;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidAccessToken;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidBookingRequest;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidPromotion;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidStateTransition;
import com.tvh.homestay.booking.exception.BookingExceptions.PromotionExhausted;
import com.tvh.homestay.booking.exception.BookingExceptions.RoomNotAvailable;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Đổi ngoại lệ bảo mật thành phản hồi {@code problem+json} đúng mã trạng thái.
 *
 * <p>Không có lớp này, {@code BadCredentialsException} ném từ tầng service rơi
 * ra ngoài thành 500 kèm stack trace — vừa sai mã, vừa lộ cấu trúc nội bộ cho
 * người đang thử đăng nhập sai.
 *
 * <p>Thông báo cố ý chung chung. "Email không tồn tại" và "sai mật khẩu" là hai
 * câu khác nhau, và sự khác nhau đó cho kẻ dò biết email nào có thật trong hệ
 * thống.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Mã trạng thái HTTP cho từng ngoại lệ nghiệp vụ.
     *
     * <p>Bảng tra thay vì một chuỗi {@code @ExceptionHandler}: thêm ngoại lệ mới
     * mà quên map thì rơi vào 400 chứ không phải 500, và chỗ cần sửa chỉ có một.
     */
    private static final Map<Class<? extends BookingException>, HttpStatus> STATUS_BY_TYPE = Map.of(
            RoomNotAvailable.class, HttpStatus.CONFLICT,
            InvalidStateTransition.class, HttpStatus.CONFLICT,
            PromotionExhausted.class, HttpStatus.CONFLICT,
            BookingNotFound.class, HttpStatus.NOT_FOUND,
            InvalidAccessToken.class, HttpStatus.UNAUTHORIZED,
            InvalidPromotion.class, HttpStatus.BAD_REQUEST,
            InvalidBookingRequest.class, HttpStatus.BAD_REQUEST);

    /**
     * Lỗi nghiệp vụ trả {@code problem+json} kèm MÃ ỔN ĐỊNH.
     *
     * <p>Frontend hiển thị thông báo tiếng Việt theo trường {@code code}, không
     * đọc chuỗi {@code detail} — đổi câu chữ ở backend không được phép làm hỏng
     * giao diện.
     */
    @ExceptionHandler(BookingException.class)
    public ProblemDetail handleBookingException(BookingException exception) {
        HttpStatus status =
                STATUS_BY_TYPE.getOrDefault(exception.getClass(), HttpStatus.BAD_REQUEST);
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(exception.getCode());
        problem.setDetail(exception.getMessage());
        problem.setProperty("code", exception.getCode());
        return problem;
    }

    /**
     * Lỗi nghiệp vụ của khu quản trị. Mã trạng thái đi kèm chính ngoại lệ,
     * không tra bảng — xem javadoc của {@code AdminExceptions}.
     */
    @ExceptionHandler(AdminException.class)
    public ProblemDetail handleAdminException(AdminException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(exception.getStatus());
        problem.setTitle(exception.getCode());
        problem.setDetail(exception.getMessage());
        problem.setProperty("code", exception.getCode());
        return problem;
    }

    /**
     * Tệp tải lên vượt giới hạn của tầng servlet.
     *
     * <p>Không có handler này, ngoại lệ rơi ra ngoài thành 500 kèm stack trace:
     * sai mã (413 mới đúng), và người dùng không hiểu vì sao ảnh của mình bị
     * từ chối. Giới hạn bị chạm Ở ĐÂY, TRƯỚC khi request tới controller, nên
     * {@code ImageValidator} không bao giờ có cơ hội nói câu đó thay.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.PAYLOAD_TOO_LARGE);
        problem.setTitle("IMAGE_TOO_LARGE");
        problem.setDetail("Tệp vượt quá dung lượng cho phép.");
        problem.setProperty("code", "IMAGE_TOO_LARGE");
        return problem;
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ProblemDetail handleAuthenticationFailure(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("UNAUTHORIZED");
        problem.setDetail(exception.getMessage());
        problem.setProperty("code", "UNAUTHORIZED");
        return problem;
    }
}
