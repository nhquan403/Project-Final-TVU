package com.tvh.homestay.admin.exception;

import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ nghiệp vụ của khu quản trị.
 *
 * <p>Khác với {@code BookingExceptions}, mỗi ngoại lệ ở đây mang LUÔN mã trạng
 * thái HTTP của nó. Lý do: nhóm này trải từ 400 tới 415 và 413, và một bảng tra
 * riêng ở {@code ApiExceptionHandler} sẽ là chỗ thứ hai phải nhớ cập nhật mỗi
 * lần thêm một loại lỗi.
 *
 * <p>Mã lỗi là HỢP ĐỒNG với giao diện: màn hình admin hiển thị thông báo theo
 * {@code code}, không đọc chuỗi {@code detail}.
 */
public final class AdminExceptions {

    private AdminExceptions() {}

    public abstract static sealed class AdminException extends RuntimeException
            permits AdminResourceNotFound, ResourceInUse, InvalidAdminRequest,
                    UnsupportedImageType, ImageTooLarge, ImageStorageFailed {

        private final String code;
        private final HttpStatus status;

        protected AdminException(HttpStatus status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    /** 404 — bản ghi không tồn tại. */
    public static final class AdminResourceNotFound extends AdminException {
        public AdminResourceNotFound(String what) {
            super(HttpStatus.NOT_FOUND, "ADMIN_RESOURCE_NOT_FOUND", "Không tìm thấy " + what + ".");
        }
    }

    /**
     * 409 — còn dữ liệu tham chiếu tới bản ghi này.
     *
     * <p>Dùng cho xoá cứng loại phòng/phòng/mã giảm giá còn đơn đang trỏ tới.
     * Thông báo phải nêu LỐI ĐI TIẾP (tắt {@code active}), không chỉ nói "không
     * xoá được" — người dùng vẫn còn việc cần làm sau khi đọc câu đó.
     */
    public static final class ResourceInUse extends AdminException {
        public ResourceInUse(String message) {
            super(HttpStatus.CONFLICT, "RESOURCE_IN_USE", message);
        }
    }

    /** 400 — dữ liệu gửi lên không hợp lệ theo nghiệp vụ. */
    public static final class InvalidAdminRequest extends AdminException {
        public InvalidAdminRequest(String message) {
            super(HttpStatus.BAD_REQUEST, "INVALID_ADMIN_REQUEST", message);
        }
    }

    /** 415 — tệp tải lên không nằm trong danh sách trắng định dạng ảnh. */
    public static final class UnsupportedImageType extends AdminException {
        public UnsupportedImageType(String message) {
            super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_IMAGE_TYPE", message);
        }
    }

    /** 413 — ảnh vượt giới hạn dung lượng. */
    public static final class ImageTooLarge extends AdminException {
        public ImageTooLarge(String message) {
            super(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", message);
        }
    }

    /** 500 — không ghi được ảnh xuống nơi lưu trữ. */
    public static final class ImageStorageFailed extends AdminException {
        public ImageStorageFailed(String message) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_STORAGE_FAILED", message);
        }
    }
}
