package com.tvh.homestay.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO của khu quản trị. Gom một chỗ theo đúng lối {@code BookingDtos} của Phase 5. */
public final class AdminDtos {

    private AdminDtos() {}

    // ─── Booking ─────────────────────────────────────────────────────────

    /** Một dòng trong bảng danh sách đơn. Cố ý gọn: bảng không cần chi tiết. */
    public record BookingRow(
            Long id,
            String code,
            String guestName,
            String guestPhone,
            String roomTypeName,
            LocalDate checkIn,
            LocalDate checkOut,
            int roomQuantity,
            BigDecimal totalAmount,
            BigDecimal depositAmount,
            String status,
            String paymentStatus,
            OffsetDateTime createdAt) {}

    public record StatusHistoryEntry(
            String fromStatus,
            String toStatus,
            String actor,
            String changedBy,
            String note,
            OffsetDateTime createdAt) {}

    public record PaymentAttemptView(
            Long id,
            int attemptNo,
            String transferContent,
            BigDecimal amountExpected,
            BigDecimal amountReceived,
            String status,
            String reconcileStatus,
            OffsetDateTime paidAt,
            OffsetDateTime expiresAt) {}

    public record OutboundEmailView(
            Long id,
            String template,
            String toEmail,
            String status,
            int attempts,
            String lastError,
            OffsetDateTime sentAt,
            OffsetDateTime createdAt) {}

    public record NoteView(Long id, String author, String content, OffsetDateTime createdAt) {}

    public record AssignedRoomView(Long roomId, String roomNumber, Integer floor, String status) {}

    /** Chi tiết đơn: đủ để admin quyết định mà không phải mở thêm màn hình nào. */
    public record BookingDetail(
            Long id,
            String code,
            String status,
            String paymentStatus,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            int roomQuantity,
            int adults,
            int children,
            String roomTypeName,
            BigDecimal unitPriceSnapshot,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal depositAmount,
            String promotionCode,
            String specialRequest,
            OffsetDateTime holdExpiresAt,
            OffsetDateTime createdAt,
            List<AssignedRoomView> rooms,
            List<StatusHistoryEntry> history,
            List<PaymentAttemptView> payments,
            List<OutboundEmailView> emails,
            List<NoteView> notes) {}

    public record TransitionRequest(
            @NotBlank String toStatus,
            @Size(max = 500) String note) {}

    public record NoteRequest(@NotBlank @Size(max = 2000) String content) {}

    public record ResendEmailRequest(@Size(max = 100) String template) {}

    // ─── Đối soát thanh toán ─────────────────────────────────────────────

    public record ReconcileRow(
            Long paymentId,
            String bookingCode,
            String guestName,
            String transferContent,
            BigDecimal amountExpected,
            BigDecimal amountReceived,
            String paymentStatus,
            String reconcileStatus,
            String bookingStatus,
            OffsetDateTime paidAt,
            /** Nguyên văn payload webhook — chứng cứ khi tranh chấp với nhà cung cấp. */
            List<String> webhookPayloads) {}

    public record ResolveRequest(@Size(max = 500) String note) {}

    // ─── Loại phòng ──────────────────────────────────────────────────────

    public record RoomTypeImageView(
            Long id, String url, String publicId, String altText, int displayOrder, boolean cover) {}

    public record AmenityView(Long id, String code, String name, String icon, String category) {}

    public record RoomTypeView(
            Long id,
            String code,
            String slug,
            String name,
            String shortDescription,
            String description,
            BigDecimal basePrice,
            int capacityAdults,
            int capacityChildren,
            String bedInfo,
            BigDecimal areaSqm,
            int displayOrder,
            boolean active,
            long roomCount,
            List<AmenityView> amenities,
            List<RoomTypeImageView> images) {}

    public record RoomTypeRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 120) String slug,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 4000) String shortDescription,
            String description,
            @NotNull @DecimalMin("1") BigDecimal basePrice,
            @Min(1) int capacityAdults,
            @Min(0) int capacityChildren,
            @Size(max = 150) String bedInfo,
            BigDecimal areaSqm,
            @Min(0) int displayOrder,
            boolean active) {}

    public record AmenityIdsRequest(@NotNull List<Long> amenityIds) {}

    public record AddImageRequest(
            @NotBlank @Size(max = 500) String url,
            @Size(max = 255) String publicId,
            @Size(max = 255) String altText) {}

    public record ImageOrderEntry(@NotNull Long id, @Min(0) int displayOrder, boolean cover) {}

    public record ImageOrderRequest(@NotNull List<ImageOrderEntry> images) {}

    // ─── Phòng vật lý ────────────────────────────────────────────────────

    public record RoomView(
            Long id,
            Long roomTypeId,
            String roomTypeName,
            String roomNumber,
            Integer floor,
            String status,
            String note) {}

    public record RoomRequest(
            @NotNull Long roomTypeId,
            @NotBlank @Size(max = 20) String roomNumber,
            Integer floor,
            @Size(max = 2000) String note) {}

    public record RoomStatusRequest(@NotBlank String status) {}

    /** Đơn sẽ bị ảnh hưởng khi đưa phòng ra khỏi vận hành. KHÔNG tự huỷ đơn nào. */
    public record AffectedBooking(
            String code, String guestName, LocalDate checkIn, LocalDate checkOut, String status) {}

    public record RoomStatusResult(RoomView room, List<AffectedBooking> affectedBookings) {}

    // ─── Khuyến mãi ──────────────────────────────────────────────────────

    public record PromotionView(
            Long id,
            String code,
            String name,
            String description,
            String discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            int minNights,
            BigDecimal minTotalAmount,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            Integer usageLimit,
            int usedCount,
            boolean active,
            /** Số đơn đã dùng mã. Khác used_count khi có đơn bị huỷ và hoàn lượt. */
            long bookingCount) {}

    public record PromotionRequest(
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 150) String name,
            String description,
            @NotBlank String discountType,
            @NotNull @DecimalMin("0") BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            @Min(1) int minNights,
            @NotNull @DecimalMin("0") BigDecimal minTotalAmount,
            @NotNull OffsetDateTime startsAt,
            @NotNull OffsetDateTime endsAt,
            Integer usageLimit,
            boolean active) {}

    // ─── Đánh giá ────────────────────────────────────────────────────────

    public record ReviewView(
            Long id,
            String bookingCode,
            String guestName,
            short rating,
            String title,
            /** VĂN BẢN THUẦN. Giao diện hiển thị bằng text binding, không innerHTML. */
            String content,
            String status,
            String adminReply,
            OffsetDateTime repliedAt,
            OffsetDateTime createdAt) {}

    public record ReviewReplyRequest(@NotBlank @Size(max = 2000) String reply) {}

    // ─── Tải ảnh ─────────────────────────────────────────────────────────

    public record UploadedImage(String url, String publicId, String contentType, int bytes) {}
}
