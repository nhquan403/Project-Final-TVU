/**
 * Hình dạng dữ liệu của khu quản trị.
 *
 * Khớp một-một với `AdminDtos` và `DashboardSummary` phía backend. Tách khỏi
 * service để màn hình import kiểu mà không kéo theo cả tầng HTTP.
 */
import type { BookingStatus } from '../../shared/ui';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ─── Đơn đặt phòng ──────────────────────────────────────────────────────

export interface AdminBookingRow {
  id: number;
  code: string;
  guestName: string;
  guestPhone: string;
  roomTypeName: string;
  checkIn: string;
  checkOut: string;
  roomQuantity: number;
  totalAmount: number;
  depositAmount: number;
  status: BookingStatus;
  paymentStatus: string;
  createdAt: string;
}

export interface StatusHistoryEntry {
  fromStatus: string | null;
  toStatus: string;
  actor: string;
  changedBy: string | null;
  note: string | null;
  createdAt: string;
}

export interface PaymentAttemptView {
  id: number;
  attemptNo: number;
  transferContent: string;
  amountExpected: number;
  amountReceived: number;
  status: string;
  reconcileStatus: string;
  paidAt: string | null;
  expiresAt: string | null;
}

export interface OutboundEmailView {
  id: number;
  template: string;
  toEmail: string;
  status: string;
  attempts: number;
  lastError: string | null;
  sentAt: string | null;
  createdAt: string;
}

export interface NoteView {
  id: number;
  author: string;
  content: string;
  createdAt: string;
}

export interface AssignedRoomView {
  roomId: number;
  roomNumber: string;
  floor: number | null;
  status: string;
}

export interface AdminBookingDetail {
  id: number;
  code: string;
  status: BookingStatus;
  paymentStatus: string;
  guestName: string;
  guestEmail: string;
  guestPhone: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  roomQuantity: number;
  adults: number;
  children: number;
  roomTypeName: string;
  unitPriceSnapshot: number;
  subtotalAmount: number;
  discountAmount: number;
  totalAmount: number;
  depositAmount: number;
  promotionCode: string | null;
  specialRequest: string | null;
  holdExpiresAt: string | null;
  createdAt: string;
  rooms: AssignedRoomView[];
  history: StatusHistoryEntry[];
  payments: PaymentAttemptView[];
  emails: OutboundEmailView[];
  notes: NoteView[];
}

// ─── Đối soát thanh toán ────────────────────────────────────────────────

export interface ReconcileRow {
  paymentId: number;
  bookingCode: string;
  guestName: string;
  transferContent: string;
  amountExpected: number;
  amountReceived: number;
  paymentStatus: string;
  reconcileStatus: string;
  bookingStatus: BookingStatus;
  paidAt: string | null;
  /** Nguyên văn payload webhook — chứng cứ khi tranh chấp với nhà cung cấp. */
  webhookPayloads: string[];
}

// ─── Danh mục ───────────────────────────────────────────────────────────

export interface AmenityView {
  id: number;
  code: string;
  name: string;
  icon: string | null;
  category: string;
}

export interface RoomTypeImageView {
  id: number;
  url: string;
  publicId: string | null;
  altText: string | null;
  displayOrder: number;
  cover: boolean;
}

export interface RoomTypeView {
  id: number;
  code: string;
  slug: string;
  name: string;
  shortDescription: string | null;
  description: string | null;
  basePrice: number;
  capacityAdults: number;
  capacityChildren: number;
  bedInfo: string | null;
  areaSqm: number | null;
  displayOrder: number;
  active: boolean;
  roomCount: number;
  amenities: AmenityView[];
  images: RoomTypeImageView[];
}

export type RoomTypeRequest = Omit<RoomTypeView, 'id' | 'roomCount' | 'amenities' | 'images'>;

export interface RoomView {
  id: number;
  roomTypeId: number;
  roomTypeName: string;
  roomNumber: string;
  floor: number | null;
  status: string;
  note: string | null;
}

export interface AffectedBooking {
  code: string;
  guestName: string;
  checkIn: string;
  checkOut: string;
  status: BookingStatus;
}

export interface RoomStatusResult {
  room: RoomView;
  affectedBookings: AffectedBooking[];
}

export interface PromotionView {
  id: number;
  code: string;
  name: string;
  description: string | null;
  discountType: string;
  discountValue: number;
  maxDiscountAmount: number | null;
  minNights: number;
  minTotalAmount: number;
  startsAt: string;
  endsAt: string;
  /** `null` nghĩa là KHÔNG giới hạn lượt dùng — giao diện phải nói đúng như vậy. */
  usageLimit: number | null;
  usedCount: number;
  active: boolean;
  bookingCount: number;
}

export interface ReviewView {
  id: number;
  bookingCode: string;
  guestName: string;
  rating: number;
  title: string | null;
  /** VĂN BẢN THUẦN do người ẩn danh gửi. Hiển thị bằng text binding, không innerHTML. */
  content: string | null;
  status: string;
  adminReply: string | null;
  repliedAt: string | null;
  createdAt: string;
}

export interface UploadedImage {
  url: string;
  publicId: string | null;
  contentType: string;
  bytes: number;
}

// ─── Dashboard ──────────────────────────────────────────────────────────

export interface MonthlyValue {
  month: string;
  value: number;
}

export interface TopRoomType {
  roomTypeName: string;
  bookings: number;
  bookingValue: number;
}

/** Ba chỉ số dùng ba trục thời gian khác nhau — backend gửi kèm lời giải thích. */
export interface MetricAxes {
  bookingValue: string;
  amountReceived: string;
  occupancy: string;
  newBookings: string;
}

export interface DashboardSummary {
  periodStart: string;
  periodEnd: string;
  bookingValueTotal: number;
  amountReceivedTotal: number;
  newBookings: number;
  cancelledBookings: number;
  cancellationRate: number;
  occupancyRate: number;
  reconcileCount: number;
  bookingValueByMonth: MonthlyValue[];
  amountReceivedByMonth: MonthlyValue[];
  occupancyByMonth: MonthlyValue[];
  newBookingsByMonth: MonthlyValue[];
  topRoomTypes: TopRoomType[];
  axes: MetricAxes;
}
