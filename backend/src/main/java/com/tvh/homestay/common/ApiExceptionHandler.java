package com.tvh.homestay.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ProblemDetail handleAuthenticationFailure(RuntimeException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("UNAUTHORIZED");
        problem.setDetail(exception.getMessage());
        return problem;
    }
}
