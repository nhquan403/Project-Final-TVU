package com.tvh.homestay.booking;

import com.tvh.homestay.availability.AvailabilityRepository;
import com.tvh.homestay.booking.exception.BookingExceptions.RoomNotAvailable;
import com.tvh.homestay.common.SqlStates;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Vòng thử gán phòng vật lý — bản DUY NHẤT trong toàn hệ thống.
 *
 * <h2>KHÔNG ĐƯỢC ĐẶT {@code @Transactional} LÊN LỚP NÀY</h2>
 *
 * <p>Vòng thử bắt lỗi va chạm ràng buộc chống trùng lịch rồi thử bộ phòng khác.
 * Gộp nó vào một transaction là bất khả thi ở cả ba tầng:
 *
 * <ul>
 *   <li><b>PostgreSQL:</b> mọi lỗi đưa transaction vào trạng thái aborted; lệnh
 *       kế tiếp trả {@code 25P02} chứ KHÔNG phải {@code 23P01}, nên lần thử thứ
 *       hai không bao giờ chạy.
 *   <li><b>Spring:</b> transaction bị đánh dấu rollback-only, nên lần thử THÀNH
 *       CÔNG cuối cùng cũng bị vứt đi lúc commit.
 *   <li><b>JPA:</b> {@code EntityManager} sau ngoại lệ ở trạng thái không xác
 *       định và không được dùng lại để persist.
 * </ul>
 *
 * <p>Triệu chứng nếu ai đó gộp lại: khách nhận HTTP 500 <b>dù loại phòng còn
 * phòng trống</b>. {@code BookingConcurrencyIT#multiRoom} tồn tại để bắt đúng
 * lỗi đó.
 *
 * <p>Lớp này được tách ra khỏi {@code BookingService} khi Phase 6 cần gán lại
 * phòng cho tiền về muộn. Sao chép vòng thử ra chỗ thứ hai là tạo ra hai chỗ để
 * sai khác nhau — và cái sai sẽ nằm ở nhánh ít chạy hơn, tức là nhánh cứu tiền
 * của khách.
 */
@Component
public class RoomAllocator {

    /**
     * Số lần thử tối đa. Mỗi lần thử đã loại các phòng vừa va chạm và lọc lại
     * danh sách ứng viên từ đầu, nên năm lần là quá đủ kể cả lúc cao điểm.
     */
    private static final int MAX_ATTEMPTS = 5;

    /**
     * Một lần thử ghi dữ liệu.
     *
     * <p>Cài đặt PHẢI là một phương thức {@code @Transactional(REQUIRES_NEW)}
     * trên một bean khác — gọi qua {@code this} sẽ đi vòng qua proxy của Spring
     * và mất luôn transaction riêng, tức là mất toàn bộ lý do lớp này tồn tại.
     */
    @FunctionalInterface
    public interface Attempt<T> {
        T runInNewTransaction(List<Long> roomIds);
    }

    private final AvailabilityRepository availability;

    public RoomAllocator(AvailabilityRepository availability) {
        this.availability = availability;
    }

    /**
     * Chọn đủ {@code quantity} phòng rảnh và giao cho {@code attempt} ghi lại.
     *
     * @throws RoomNotAvailable khi không đủ ứng viên, hoặc thử hết số lần cho phép
     */
    public <T> T allocate(
            Long roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut,
            int quantity,
            Attempt<T> attempt) {

        Set<Long> excluded = new HashSet<>();
        for (int round = 1; round <= MAX_ATTEMPTS; round++) {
            List<Long> candidates =
                    new ArrayList<>(availability.findFreeRoomIds(roomTypeId, checkIn, checkOut));
            candidates.removeAll(excluded);
            if (candidates.size() < quantity) {
                throw new RoomNotAvailable();
            }
            List<Long> picked = List.copyOf(candidates.subList(0, quantity));

            try {
                return attempt.runInNewTransaction(picked);
            } catch (DataIntegrityViolationException e) {
                // CHỈ 23P01 mới là "phòng vừa bị người khác lấy mất". Mọi mã
                // khác — khoá ngoại, NOT NULL, trigger bất biến — là bug thật và
                // phải nổi lên nguyên trạng, không được che thành "hết phòng".
                if (!SqlStates.isExclusionViolation(e)) {
                    throw e;
                }
                excluded.addAll(picked);
            }
        }
        throw new RoomNotAvailable();
    }
}
