package com.tvh.homestay.admin;

import com.tvh.homestay.auth.AuthenticatedUser;

/**
 * Lấy ĐỊNH DANH của quản trị viên đang thao tác từ token.
 *
 * <h2>Vì sao trả về id chứ không trả về thực thể {@code User}</h2>
 *
 * <p>Bản đầu nạp sẵn {@code User} ở controller rồi truyền xuống service. Vì
 * {@code open-in-view} đã tắt, lượt nạp đó chạy trong transaction riêng của nó
 * và trả về một thực thể ĐÃ TÁCH KHỎI phiên. Gắn thực thể tách rời ấy vào một
 * dòng nhật ký bên trong transaction khác rồi cũng trong transaction đó lại nạp
 * chính người dùng ấy bằng {@code join fetch} khiến Hibernate ném
 * {@code AssertionFailure: possible non-threadsafe access to the session} —
 * một thông báo dẫn người đọc đi sai hướng hoàn toàn (tưởng có vấn đề đa luồng,
 * trong khi chỉ là một thực thể đi lạc giữa hai phiên).
 *
 * <p>Truyền id thì mỗi service tự nạp người dùng TRONG transaction của mình,
 * nên chỉ có một phiên quản lý thực thể đó.
 *
 * <p>Danh tính LUÔN lấy từ token, không bao giờ từ thân request.
 */
public final class CurrentAdmin {

    private CurrentAdmin() {}

    /** {@code null} khi request không có principal (không xảy ra sau chuỗi filter). */
    public static Long idOf(AuthenticatedUser principal) {
        return principal == null ? null : principal.id();
    }
}
