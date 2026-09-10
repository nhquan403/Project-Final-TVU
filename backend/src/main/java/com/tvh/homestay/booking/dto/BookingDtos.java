package com.tvh.homestay.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO của luồng đặt phòng. */
public final class BookingDtos {

    private BookingDtos() {}

    /**
     * Yêu cầu tạo đơn.
     *
     * <p>KHÔNG có trường số tiền nào: giá, giảm giá và tiền cọc đều do backend
     * tính. Nhận số tiền từ client rồi tin là mở đường sửa giá trên trình duyệt.
     */
    public record CreateBookingRequest(
            @NotNull Long roomTypeId,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            @Min(1) int roomQuantity,
            @Min(1) int adults,
            @Min(0) int children,
            @NotBlank @Size(max = 150) String guestName,
            @NotBlank @Email @Size(max = 255) String guestEmail,
            @NotBlank @Pattern(regexp = "^[0-9+ .-]{8,20}$", message = "Số điện thoại không hợp lệ")
                    String guestPhone,
            @Size(max = 2000) String specialRequest,
            @Size(max = 50) String promotionCode) {}

    /** Tra cứu đơn bằng mã và số điện thoại. */
    public record LookupRequest(
            @NotBlank @Size(max = 20) String code,
            @NotBlank @Size(max = 20) String phone) {}

    public record CancelRequest(@Size(max = 20) String phone, @Size(max = 500) String reason) {}

    public record BookingRoomView(Long roomId, String roomNumber, LocalDate checkIn, LocalDate checkOut) {}

    /** Đơn đặt phòng trả về cho khách. */
    public record BookingResponse(
            String code,
            /**
             * Chỉ có mặt ngay sau khi tạo đơn hoặc tra cứu thành công. Đây là bí
             * mật thao tác, không hiển thị ở danh sách.
             */
            String accessToken,
            String status,
            String paymentStatus,
            String roomTypeName,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            int roomQuantity,
            int adults,
            int children,
            String guestName,
            String guestPhone,
            BigDecimal pricePerNight,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            BigDecimal depositAmount,
            String transferContent,
            OffsetDateTime holdExpiresAt,
            List<BookingRoomView> rooms) {}

    /** Trạng thái thanh toán cho màn hình QR hỏi liên tục. */
    public record PaymentStatusResponse(
            String code,
            String status,
            String paymentStatus,
            BigDecimal amountExpected,
            BigDecimal amountReceived,
            OffsetDateTime holdExpiresAt) {}
}
