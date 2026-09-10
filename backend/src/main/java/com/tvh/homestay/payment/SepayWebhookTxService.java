package com.tvh.homestay.payment;

import com.tvh.homestay.booking.BookingStateMachine;
import com.tvh.homestay.booking.entity.Booking;
import com.tvh.homestay.booking.entity.BookingStatus;
import com.tvh.homestay.booking.entity.HistoryActor;
import com.tvh.homestay.booking.entity.PaymentStatus;
import com.tvh.homestay.booking.repository.BookingRepository;
import com.tvh.homestay.mail.EmailOutboxService;
import com.tvh.homestay.mail.MailService;
import com.tvh.homestay.payment.dto.SepayWebhookPayload;
import com.tvh.homestay.payment.entity.Payment;
import com.tvh.homestay.payment.entity.PaymentAttemptStatus;
import com.tvh.homestay.payment.entity.PaymentWebhookEvent;
import com.tvh.homestay.payment.entity.ReconcileStatus;
import com.tvh.homestay.payment.entity.WebhookProcessingResult;
import com.tvh.homestay.payment.repository.PaymentRepository;
import com.tvh.homestay.payment.repository.PaymentWebhookEventRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phần CÓ transaction của việc xử lý webhook SePay.
 *
 * <p>Tách khỏi {@link SepayWebhookService} vì hai việc có vòng đời transaction
 * khác nhau: {@link #claim} phải commit ĐỘC LẬP để dấu vết còn lại kể cả khi
 * {@link #process} hỏng, còn {@link #process} phải rollback trọn vẹn khi hỏng
 * để không để lại nửa khoản tiền đã ghi nhận.
 */
@Service
public class SepayWebhookTxService {

    private static final Logger log = LoggerFactory.getLogger(SepayWebhookTxService.class);

    public static final String PROVIDER = "SEPAY";

    /**
     * Regex NEO hai đầu cho nội dung chuyển khoản.
     *
     * <p>Bảng chữ là Crockford Base32 của {@code BookingCodeGenerator} (đã bỏ
     * I, L, O, U), cộng hai chữ số thứ tự lần thử. Neo {@code \b} hai đầu là
     * bắt buộc: không có nó, một chuỗi dài hơn vô tình chứa mã — chẳng hạn mã
     * tham chiếu của ngân hàng — cũng khớp, và tiền của người này được ghi cho
     * đơn của người khác.
     */
    private static final Pattern TRANSFER_CODE =
            Pattern.compile("\\bTVH[0-9A-HJKMNP-TV-Z]{6}\\d{2}\\b");

    /**
     * Dung sai ±1.000đ khi so tiền.
     *
     * <p>Có thật trong thực tế: một số ngân hàng trừ phí chuyển khoản vào số
     * tiền, và khách gõ tay hay làm tròn. Bắt khớp tuyệt đối sẽ đẩy phần lớn
     * giao dịch hợp lệ vào hàng đợi đối soát thủ công.
     */
    private static final BigDecimal TOLERANCE = new BigDecimal("1000");

    private final PaymentWebhookEventRepository events;
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final BookingStateMachine stateMachine;
    private final PaymentReallocationService reallocation;
    private final EmailOutboxService outbox;
    private final VietQrGenerator qr;
    private final Clock clock;
    private final long partialReviewHours;

    public SepayWebhookTxService(
            PaymentWebhookEventRepository events,
            PaymentRepository payments,
            BookingRepository bookings,
            BookingStateMachine stateMachine,
            PaymentReallocationService reallocation,
            EmailOutboxService outbox,
            VietQrGenerator qr,
            Clock clock,
            @Value("${payments.partial-review-hours:24}") long partialReviewHours) {
        this.events = events;
        this.payments = payments;
        this.bookings = bookings;
        this.stateMachine = stateMachine;
        this.reallocation = reallocation;
        this.outbox = outbox;
        this.qr = qr;
        this.clock = clock;
        this.partialReviewHours = partialReviewHours;
    }

    /** Kết quả của bước giành quyền xử lý một sự kiện. */
    public record Claim(Long eventId, boolean alreadyDone) {}

    /**
     * Ghi nhận sự kiện và giành quyền xử lý nó.
     *
     * <p><b>Idempotency ở đây PHÂN BIỆT KẾT QUẢ, không chỉ phân biệt đã-thấy.</b>
     * Chèn khoá rồi bỏ qua mọi lần sau là cách làm tưởng đúng: một lỗi tạm thời
     * — mất kết nối cơ sở dữ liệu đúng lúc, SMTP treo — sẽ khoá vĩnh viễn khoản
     * tiền đó khỏi mọi lần thử lại của nhà cung cấp, và tiền nằm trong tài khoản
     * mà đơn thì mãi {@code PENDING_PAYMENT}. Nên chỉ {@code MATCHED},
     * {@code LATE}, {@code UNMATCHED}, {@code DUPLICATE} mới là "xong"; còn
     * {@code ERROR} là lời mời xử lý lại.
     *
     * <p>Dòng vừa chèn mang sẵn {@code ERROR}: nếu tiến trình chết ngay sau đó,
     * trạng thái còn lại đúng bằng sự thật — đã nhận, chưa xử lý xong, và lần
     * gửi lại sẽ xử lý tiếp.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim claim(String externalId, String rawPayload) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        PaymentWebhookEvent event = events.findByProviderAndExternalId(PROVIDER, externalId)
                .orElse(null);

        if (event != null) {
            if (event.getProcessingResult() != WebhookProcessingResult.ERROR) {
                log.info("Webhook {} đã xử lý trước đó với kết quả {} — bỏ qua",
                        externalId, event.getProcessingResult());
                return new Claim(event.getId(), true);
            }
            event.setPayload(rawPayload);
            event.setErrorMessage("Đang xử lý lại sau lần hỏng trước");
            event.setProcessedAt(null);
            return new Claim(events.save(event).getId(), false);
        }

        PaymentWebhookEvent fresh = new PaymentWebhookEvent();
        fresh.setProvider(PROVIDER);
        fresh.setExternalId(externalId);
        fresh.setPayload(rawPayload);
        fresh.setProcessingResult(WebhookProcessingResult.ERROR);
        fresh.setErrorMessage("Đã nhận, chưa xử lý xong");
        fresh.setReceivedAt(now);
        // saveAndFlush để va chạm khoá duy nhất (hai webhook trùng về cùng lúc)
        // lộ ra NGAY ở đây, nơi người gọi biết cách diễn giải nó là "trùng".
        return new Claim(events.saveAndFlush(fresh).getId(), false);
    }

    /**
     * Năm lớp kiểm tra và bảng quyết định sáu nhánh.
     *
     * <p>Chạy trong transaction RIÊNG: khi có gì hỏng, mọi thay đổi ở đây biến
     * mất nhưng dòng sự kiện do {@link #claim} tạo thì còn — đúng thứ tự cần
     * thiết để lần gửi lại xử lý lại được từ đầu.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookProcessingResult process(Long eventId, SepayWebhookPayload payload) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        PaymentWebhookEvent event = events.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Không còn sự kiện webhook " + eventId));

        // ─── Lớp 2: CHIỀU TIỀN ────────────────────────────────────────────
        // Thiếu lớp này, một giao dịch CHUYỂN ĐI có nội dung chứa mã đơn — ví
        // dụ nhân viên hoàn tiền cho khách khác rồi gõ mã vào nội dung cho dễ
        // đối soát — sẽ xác nhận đơn trong khi tiền đang đi RA khỏi tài khoản.
        if (!"in".equals(payload.normalizedTransferType())) {
            log.warn("Webhook {} không phải tiền vào (transferType={}) — không xác nhận đơn nào",
                    event.getExternalId(), payload.transferType());
            return finish(event, WebhookProcessingResult.UNMATCHED,
                    "Chiều tiền không phải 'in': " + payload.transferType(), now);
        }

        // ─── Lớp 3: TÀI KHOẢN ĐÍCH ────────────────────────────────────────
        // Chưa cấu hình tài khoản nhận thì TỪ CHỐI, không phải bỏ qua kiểm tra:
        // mở sẵn đường cho mọi giao dịch khi thiếu cấu hình là biến một lỗi
        // triển khai thành một lỗ hổng.
        if (!receivingAccountMatches(payload)) {
            log.warn("Webhook {} vào tài khoản không phải tài khoản nhận cọc — không xác nhận đơn nào",
                    event.getExternalId());
            return finish(event, WebhookProcessingResult.UNMATCHED,
                    "Số tài khoản nhận không khớp cấu hình", now);
        }

        // ─── Lớp 5a: KHỚP MÃ, VÀ PHẢI ĐÚNG MỘT MÃ ─────────────────────────
        Set<String> found = extractTransferContents(payload.content());
        if (found.size() != 1) {
            return finish(event, WebhookProcessingResult.UNMATCHED,
                    found.isEmpty()
                            ? "Nội dung chuyển khoản không chứa mã đơn nào"
                            : "Nội dung chuyển khoản chứa nhiều mã: " + found,
                    now);
        }
        String transferContent = found.iterator().next();

        Payment payment = payments.lockByTransferContent(transferContent).orElse(null);
        if (payment == null) {
            return finish(event, WebhookProcessingResult.UNMATCHED,
                    "Không có lần thanh toán nào mang nội dung " + transferContent, now);
        }
        event.setPayment(payment);

        // Khoá dòng đơn TRƯỚC khi đọc trạng thái. Bộ quét hết hạn có thể đang
        // chạy ngay lúc này; đọc trước rồi mới khoá là đọc phải trạng thái cũ.
        Booking booking = bookings.lockById(payment.getBooking().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Lần thanh toán " + payment.getId() + " không còn đơn tương ứng"));

        return applyDecision(event, payment, booking, payload, now);
    }

    /** Ghi lỗi cho một sự kiện mà không đánh dấu đã xử lý xong. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordError(Long eventId, String message) {
        events.findById(eventId).ifPresent(event -> {
            event.setProcessingResult(WebhookProcessingResult.ERROR);
            event.setErrorMessage(message);
            event.setProcessedAt(null);
            events.save(event);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Bảng quyết định
    // ─────────────────────────────────────────────────────────────────────

    private WebhookProcessingResult applyDecision(
            PaymentWebhookEvent event,
            Payment payment,
            Booking booking,
            SepayWebhookPayload payload,
            OffsetDateTime now) {

        BookingStatus statusBefore = booking.getStatus();
        boolean wasClosed =
                statusBefore == BookingStatus.EXPIRED || statusBefore == BookingStatus.CANCELLED;

        // ─── Lớp 5b: SỐ TIỀN, CỘNG DỒN ────────────────────────────────────
        // Cộng dồn chứ không ghi đè: khách chuyển thiếu rồi bù là chuyện bình
        // thường, và ghi đè sẽ xoá mất lần chuyển đầu.
        BigDecimal received = payment.getAmountReceived().add(payload.amountOrZero());
        BigDecimal expected = payment.getAmountExpected();
        payment.setAmountReceived(received);
        if (payload.referenceCode() != null && !payload.referenceCode().isBlank()) {
            payment.setProviderTxnId(payload.referenceCode());
        }

        boolean enough = received.compareTo(expected.subtract(TOLERANCE)) >= 0;
        boolean overpaid = received.compareTo(expected.add(TOLERANCE)) > 0;

        if (!enough) {
            return handlePartial(event, payment, booking, received, expected, wasClosed, now);
        }

        payment.setStatus(overpaid ? PaymentAttemptStatus.OVERPAID : PaymentAttemptStatus.SUCCEEDED);
        payment.setPaidAt(now);
        payment.setReconcileStatus(
                overpaid ? ReconcileStatus.REFUND_REQUIRED : ReconcileStatus.NONE);

        if (wasClosed) {
            return handleLateMoney(event, payment, booking, received, overpaid, now);
        }
        if (statusBefore == BookingStatus.PENDING_PAYMENT
                || statusBefore == BookingStatus.AWAITING_REVIEW) {
            confirm(booking, received, overpaid, now);
            payments.save(payment);
            return finish(event, WebhookProcessingResult.MATCHED,
                    overpaid ? "Đã xác nhận, thừa tiền — cần hoàn lại" : "Đã xác nhận đơn", now);
        }

        // Đơn đã đi tiếp (CONFIRMED, CHECKED_IN, …) mà tiền lại về thêm. Không
        // đổi trạng thái đơn — chỉ đánh dấu cần hoàn tiền để người xử lý thấy.
        payment.setReconcileStatus(ReconcileStatus.REFUND_REQUIRED);
        booking.setPaymentStatus(PaymentStatus.OVERPAID);
        bookings.save(booking);
        payments.save(payment);
        log.warn("Đơn {} đang ở {} nhưng nhận thêm tiền — đưa vào hàng đợi hoàn tiền",
                booking.getCode(), statusBefore);
        return finish(event, WebhookProcessingResult.MATCHED,
                "Đơn đã ở trạng thái " + statusBefore + ", khoản tiền này cần hoàn lại", now);
    }

    /** Thiếu tiền: giữ chỗ thêm 24h và đẩy vào hàng đợi đối soát. */
    private WebhookProcessingResult handlePartial(
            PaymentWebhookEvent event,
            Payment payment,
            Booking booking,
            BigDecimal received,
            BigDecimal expected,
            boolean wasClosed,
            OffsetDateTime now) {

        OffsetDateTime extended = now.plusHours(partialReviewHours);
        payment.setStatus(PaymentAttemptStatus.PARTIAL);
        payment.setReconcileStatus(ReconcileStatus.NEEDS_REVIEW);
        payment.setExpiresAt(extended);
        payments.save(payment);

        moveToAwaitingReview(booking, "Chuyển khoản chưa đủ tiền cọc — chờ đối soát");
        booking.setPaymentStatus(PaymentStatus.PARTIAL);
        // Gia hạn giữ chỗ +24h. Bộ quét hết hạn của Phase 5 chỉ đụng đơn
        // PENDING_PAYMENT chưa nhận đồng nào, nên đơn này đã nằm ngoài tầm nó;
        // gia hạn ở đây là để MÀN HÌNH của khách nói đúng sự thật rằng phòng
        // vẫn đang được giữ, thay vì đếm ngược về 0 rồi báo hết hạn.
        booking.setHoldExpiresAt(extended);
        bookings.save(booking);

        BigDecimal missing = expected.subtract(received).max(BigDecimal.ZERO);
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("amountReceived", EmailOutboxService.money(received));
        extra.put("amountMissing", EmailOutboxService.money(missing));
        extra.put("transferContent", payment.getTransferContent());
        outbox.enqueue(booking, MailService.TEMPLATE_PARTIAL, extra);

        return finish(event,
                wasClosed ? WebhookProcessingResult.LATE : WebhookProcessingResult.MATCHED,
                "Nhận " + received.toPlainString() + " / cần " + expected.toPlainString()
                        + " — giữ chỗ tới " + extended,
                now);
    }

    /** Đủ tiền nhưng đơn đã đóng: thử giành lại phòng, hết phòng thì chuyển người xử lý. */
    private WebhookProcessingResult handleLateMoney(
            PaymentWebhookEvent event,
            Payment payment,
            Booking booking,
            BigDecimal received,
            boolean overpaid,
            OffsetDateTime now) {

        boolean regained = reallocation.reassign(booking);
        if (regained) {
            confirm(booking, received, overpaid, now);
            payments.save(payment);
            return finish(event, WebhookProcessingResult.LATE,
                    "Tiền về sau khi đơn đã đóng — đã giành lại phòng và xác nhận", now);
        }

        // Tiền đã vào tài khoản nhưng không còn phòng. Đơn dừng ở
        // AWAITING_REVIEW và lần thanh toán mang NEEDS_REVIEW — hai dấu hiệu
        // độc lập để không nhánh nào của màn hình đối soát bỏ sót nó.
        payment.setReconcileStatus(ReconcileStatus.NEEDS_REVIEW);
        payments.save(payment);
        booking.setPaymentStatus(
                overpaid ? PaymentStatus.OVERPAID : PaymentStatus.DEPOSIT_PAID);
        bookings.save(booking);
        return finish(event, WebhookProcessingResult.LATE,
                "Tiền về sau khi đơn đã đóng và KHÔNG còn phòng — chờ người xử lý", now);
    }

    private void confirm(
            Booking booking, BigDecimal received, boolean overpaid, OffsetDateTime now) {
        stateMachine.transition(
                booking, BookingStatus.CONFIRMED, HistoryActor.SYSTEM, "Đã nhận đủ tiền cọc");
        booking.setPaymentStatus(depositStatus(booking, received, overpaid));
        bookings.save(booking);
        outbox.enqueue(booking, MailService.TEMPLATE_CONFIRMED, Map.of());
    }

    private void moveToAwaitingReview(Booking booking, String reason) {
        if (BookingStateMachine.canTransition(booking.getStatus(), BookingStatus.AWAITING_REVIEW)) {
            stateMachine.transition(
                    booking, BookingStatus.AWAITING_REVIEW, HistoryActor.SYSTEM, reason);
        }
    }

    private static PaymentStatus depositStatus(
            Booking booking, BigDecimal received, boolean overpaid) {
        if (overpaid) {
            return PaymentStatus.OVERPAID;
        }
        return received.compareTo(booking.getTotalAmount()) >= 0
                ? PaymentStatus.PAID
                : PaymentStatus.DEPOSIT_PAID;
    }

    private WebhookProcessingResult finish(
            PaymentWebhookEvent event,
            WebhookProcessingResult result,
            String message,
            OffsetDateTime now) {
        event.setProcessingResult(result);
        event.setErrorMessage(message);
        event.setProcessedAt(now);
        events.save(event);
        return result;
    }

    private boolean receivingAccountMatches(SepayWebhookPayload payload) {
        String configured = qr.getAccountNumber();
        if (configured == null || configured.isBlank()) {
            return false;
        }
        return configured.equals(trimmed(payload.accountNumber()))
                || configured.equals(trimmed(payload.subAccount()));
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    /** Mọi mã KHÁC NHAU tìm được trong nội dung chuyển khoản. */
    static Set<String> extractTransferContents(String content) {
        Set<String> found = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return found;
        }
        Matcher matcher = TRANSFER_CODE.matcher(content.toUpperCase(java.util.Locale.ROOT));
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }
}
