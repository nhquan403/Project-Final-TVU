package com.tvh.homestay.review;

import com.tvh.homestay.booking.BookingService;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.exception.BookingExceptions.InvalidBookingRequest;
import com.tvh.homestay.review.dto.ReviewDtos.PublicReview;
import com.tvh.homestay.review.dto.ReviewDtos.SubmitReviewRequest;
import com.tvh.homestay.review.entity.Review;
import com.tvh.homestay.review.entity.ReviewStatus;
import com.tvh.homestay.review.repository.ReviewRepository;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhận đánh giá của khách và phục vụ những đánh giá ĐÃ ĐƯỢC DUYỆT.
 *
 * <h2>Vì sao gửi đánh giá phải kiểm danh tính</h2>
 *
 * <p>Mã đơn chỉ chín ký tự, in trên email, đọc qua điện thoại, và nằm trong nội
 * dung chuyển khoản nên xuất hiện trên sao kê ngân hàng lẫn dashboard của nhà
 * cung cấp thanh toán. Nếu chỉ cần mã là gửi được đánh giá, thì bất kỳ ai nhìn
 * thấy sao kê đều đăng được một đánh giá một sao ĐỨNG TÊN khách thật — và tên
 * đó là tên thật, vì hệ thống tự chụp từ đơn.
 *
 * <p>Nên đường vào là {@code accessToken} HOẶC số điện thoại, đúng mức xác thực
 * đã dùng cho tra cứu và huỷ đơn. Dùng lại
 * {@code BookingService.requireByCodeAndTokenOrPhone} thay vì viết lớp kiểm thứ
 * hai: hai lớp kiểm là hai lớp để lệch nhau.
 */
@Service
public class ReviewService {

    /** Số đánh giá nhúng vào trang chủ. */
    private static final int PUBLIC_LIMIT = 12;

    private final ReviewRepository reviews;
    private final BookingService bookings;

    public ReviewService(ReviewRepository reviews, BookingService bookings) {
        this.reviews = reviews;
        this.bookings = bookings;
    }

    /**
     * @throws com.tvh.homestay.booking.exception.BookingExceptions.BookingNotFound
     *     khi mã sai, hoặc khi cả token lẫn số điện thoại đều không khớp — CÙNG
     *     một lỗi cho cả hai, để không ai dò được mã nào có thật
     */
    @Transactional
    public PublicReview submit(String code, SubmitReviewRequest request) {
        Booking booking =
                bookings.requireByCodeAndTokenOrPhone(code, request.token(), request.phone());

        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            throw new InvalidBookingRequest(
                    "Chỉ đánh giá được sau khi đã trả phòng. Đơn hiện đang ở trạng thái "
                            + booking.getStatus() + ".");
        }
        if (reviews.existsByBookingId(booking.getId())) {
            // Cột booking_id có UNIQUE nên cơ sở dữ liệu vẫn là chốt chặn cuối;
            // kiểm ở đây chỉ để trả về một câu tiếng Việt thay vì lỗi ràng buộc.
            throw new InvalidBookingRequest("Đơn này đã có đánh giá rồi.");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setUser(booking.getUser());
        // Tên lấy từ ĐƠN, không lấy từ thân request.
        review.setGuestNameSnapshot(booking.getGuestName());
        review.setRating(request.rating());
        // Lưu NGUYÊN VĂN. Đây là văn bản thuần; nơi hiển thị dùng text binding.
        review.setTitle(request.title());
        review.setContent(request.content());
        // Chờ duyệt. Không có bước này thì nội dung của người ẩn danh ra thẳng
        // trang chủ ngay khi gửi.
        review.setStatus(ReviewStatus.PENDING);
        return toView(reviews.save(review));
    }

    @Transactional(readOnly = true)
    public List<PublicReview> published() {
        return reviews.findByStatusOrderByIdDesc(ReviewStatus.APPROVED, Limit.of(PUBLIC_LIMIT))
                .stream()
                .map(ReviewService::toView)
                .toList();
    }

    /** Đơn này đã đánh giá chưa — để trang đánh giá không mở form cho một đơn đã gửi. */
    @Transactional(readOnly = true)
    public boolean alreadyReviewed(Long bookingId) {
        return reviews.existsByBookingId(bookingId);
    }

    private static PublicReview toView(Review review) {
        return new PublicReview(
                review.getId(),
                review.getGuestNameSnapshot(),
                review.getRating(),
                review.getTitle(),
                review.getContent(),
                review.getBooking().getRoomTypeNameSnapshot(),
                review.getAdminReply(),
                review.getCreatedAt());
    }
}
