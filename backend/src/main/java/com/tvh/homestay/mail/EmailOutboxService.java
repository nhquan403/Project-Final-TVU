package com.tvh.homestay.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.payment.entity.OutboundEmail;
import com.tvh.homestay.payment.entity.OutboundEmailStatus;
import com.tvh.homestay.payment.repository.OutboundEmailRepository;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xếp thư vào hộp thư đi, TRONG CÙNG transaction với nghiệp vụ sinh ra nó.
 *
 * <p>Đây là điểm khác biệt so với việc gửi thẳng bằng {@code @Async} hay
 * {@code @TransactionalEventListener}. Gửi thẳng có hai đường hỏng, cả hai đều
 * im lặng: SMTP chậm 20 giây thì webhook trả về sau 20 giây và nhà cung cấp coi
 * là quá hạn rồi gửi lại; SMTP hỏng thì lá thư biến mất, không ai biết, không
 * gửi lại được. Ghi một dòng vào bảng thì webhook trả về ngay, và câu hỏi "đã
 * gửi chưa" trả lời được bằng SQL.
 *
 * <p>{@code MANDATORY} là chủ ý: gọi ngoài transaction sẽ ném ngay lúc chạy
 * thay vì lặng lẽ tạo một transaction riêng — mà transaction riêng nghĩa là
 * thư vẫn được xếp hàng kể cả khi nghiệp vụ rollback.
 */
@Service
public class EmailOutboxService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final OutboundEmailRepository outbox;
    private final ObjectMapper objectMapper;
    private final String contactPhone;
    private final String contactEmail;
    private final String contactAddress;

    public EmailOutboxService(
            OutboundEmailRepository outbox,
            ObjectMapper objectMapper,
            @Value("${homestay.contact.phone:0294 3855 246}") String contactPhone,
            @Value("${homestay.contact.email:lienhe@homestaytvh.vn}") String contactEmail,
            @Value("${homestay.contact.address:Trà Vinh}") String contactAddress) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.contactAddress = contactAddress;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboundEmail enqueue(Booking booking, String template, Map<String, Object> extra) {
        Map<String, Object> payload = basePayload(booking);
        if (extra != null) {
            payload.putAll(extra);
        }

        OutboundEmail email = new OutboundEmail();
        email.setBooking(booking);
        email.setTemplate(template);
        email.setToEmail(booking.getGuestEmail());
        email.setPayload(writeJson(payload));
        email.setStatus(OutboundEmailStatus.PENDING);
        return outbox.save(email);
    }

    /**
     * Dữ liệu của một lá thư được CHỐT tại lúc xếp hàng, không tra lại lúc gửi.
     *
     * <p>Thư nói "đơn của bạn đã được xác nhận" mà lúc gửi mới đọc trạng thái
     * thì một đơn bị huỷ trong lúc chờ sẽ sinh ra lá thư tự mâu thuẫn với chính
     * nó. Chốt tại lúc xếp hàng cũng khiến bảng này thành bằng chứng đúng nghĩa:
     * nó ghi lại điều đã được nói với khách, không phải điều đang đúng hôm nay.
     */
    private Map<String, Object> basePayload(Booking booking) {
        Map<String, Object> payload = new LinkedHashMap<>();
        BigDecimal deposit = booking.getDepositAmount();
        BigDecimal remaining = booking.getTotalAmount().subtract(deposit).max(BigDecimal.ZERO);

        payload.put("code", booking.getCode());
        payload.put("guestName", booking.getGuestName());
        payload.put("roomTypeName", booking.getRoomTypeNameSnapshot());
        payload.put("checkIn", booking.getCheckIn().format(DATE));
        payload.put("checkOut", booking.getCheckOut().format(DATE));
        payload.put("nights", ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut()));
        payload.put("roomQuantity", booking.getRoomQuantity());
        payload.put("adults", booking.getAdults());
        payload.put("children", booking.getChildren());
        payload.put("totalAmount", money(booking.getTotalAmount()));
        payload.put("depositAmount", money(deposit));
        payload.put("remainingAmount", money(remaining));
        payload.put("holdExpiresAt", formatMoment(booking.getHoldExpiresAt()));
        payload.put("contactPhone", contactPhone);
        payload.put("contactEmail", contactEmail);
        payload.put("contactAddress", contactAddress);
        return payload;
    }

    /**
     * Xếp hàng LẠI một lá thư đã gửi hỏng hoặc khách bảo không nhận được.
     *
     * <p>Tạo một dòng MỚI thay vì đặt dòng cũ về {@code PENDING}. Dòng cũ là
     * bằng chứng: nó ghi lần gửi đó đã hỏng vì lý do gì và vào lúc nào. Đặt lại
     * nó về PENDING sẽ xoá sạch {@code attempts} và {@code last_error} — đúng
     * thứ cần đọc khi tìm hiểu vì sao khách không nhận được thư.
     *
     * <p>Nội dung được sao y bản cũ, không dựng lại từ đơn hiện tại: lá thư gửi
     * lại phải nói đúng điều đã nói lần đầu.
     */
    @Transactional
    public OutboundEmail requeue(OutboundEmail original) {
        OutboundEmail copy = new OutboundEmail();
        copy.setBooking(original.getBooking());
        copy.setTemplate(original.getTemplate());
        copy.setToEmail(original.getToEmail());
        copy.setPayload(original.getPayload());
        copy.setStatus(OutboundEmailStatus.PENDING);
        return outbox.save(copy);
    }

    /** Lô thư kế tiếp cần gửi. Chỉ đọc, để bộ gửi không giữ transaction lúc nói chuyện với SMTP. */
    @Transactional(readOnly = true)
    public java.util.List<OutboundEmail> nextPending(int batchSize) {
        return outbox.findByStatusOrderByIdAsc(
                OutboundEmailStatus.PENDING, org.springframework.data.domain.Limit.of(batchSize));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Map<String, Object>> readPayload(OutboundEmail email) {
        try {
            return java.util.Optional.of(objectMapper.readValue(
                    email.getPayload(), new com.fasterxml.jackson.core.type.TypeReference<>() {}));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    @Transactional
    public void markSent(Long id, OffsetDateTime sentAt) {
        outbox.findById(id).ifPresent(email -> {
            email.setStatus(OutboundEmailStatus.SENT);
            email.setAttempts(email.getAttempts() + 1);
            email.setSentAt(sentAt);
            email.setLastError(null);
            outbox.save(email);
        });
    }

    /**
     * Ghi nhận một lần gửi hỏng.
     *
     * <p>Quá {@code maxAttempts} thì chuyển {@code FAILED} và ngừng thử: thư
     * gửi tới một địa chỉ sai sẽ hỏng mãi mãi, và thử lại vô hạn chỉ làm hàng
     * đợi tắc và che mất những thư còn cứu được.
     */
    @Transactional
    public void markFailure(Long id, String error, int maxAttempts) {
        outbox.findById(id).ifPresent(email -> {
            int attempts = email.getAttempts() + 1;
            email.setAttempts(attempts);
            email.setLastError(error == null ? "(không rõ)" : error);
            if (attempts >= maxAttempts) {
                email.setStatus(OutboundEmailStatus.FAILED);
            }
            outbox.save(email);
        });
    }

    /** Định dạng tiền kiểu Việt Nam: dấu chấm ngăn nhóm nghìn, không phần lẻ. */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount) + " đ";
    }

    private static String formatMoment(OffsetDateTime moment) {
        return moment == null ? "" : moment.format(DATE_TIME);
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Không tuần tự hoá được nội dung thư", e);
        }
    }
}
