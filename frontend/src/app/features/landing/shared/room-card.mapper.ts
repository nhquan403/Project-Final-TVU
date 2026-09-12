import type { RoomCardData } from '../../../shared/ui';
import type { RoomTypeAvailability } from '../../../core/services/availability.service';

/**
 * Ảnh dự phòng khi loại phòng chưa có ảnh nào.
 *
 * Tệp tĩnh trong repo, KHÔNG phải URL ngoài: bản đem đi trình diễn không được
 * phụ thuộc vào một máy chủ ảnh của người khác còn sống hay không.
 */
export const ROOM_PLACEHOLDER_IMAGE = '/images/room-placeholder.svg';

/**
 * Đổi dữ liệu API thành dữ liệu `ui-room-card`.
 *
 * `ui-room-card` (Phase 2) đòi `id: string`, `imageUrl: string` và
 * `imageAlt: string` — cả ba đều bắt buộc. API trả `roomTypeId` là số và
 * `coverImageUrl` có thể null (loại phòng chưa ai gắn ảnh). Không có lớp ánh xạ
 * này thì thẻ phòng hiện ảnh vỡ đúng ở trang bán hàng, và Angular đưa chuỗi
 * "null" vào thuộc tính `src`.
 *
 * Để ở MỘT chỗ vì ba màn hình dùng chung thẻ này: trang chủ, danh sách phòng,
 * và bước chọn phòng trong luồng đặt.
 */
export function toRoomCard(room: RoomTypeAvailability, nights: number): RoomCardData {
  return {
    id: String(room.roomTypeId),
    name: room.name,
    imageUrl: room.coverImageUrl ?? ROOM_PLACEHOLDER_IMAGE,
    imageAlt: room.coverImageUrl ? `Ảnh ${room.name}` : `Chưa có ảnh cho ${room.name}`,
    capacity: room.capacityAdults,
    bedSummary: room.bedInfo ?? 'Liên hệ để biết thêm',
    topAmenities: room.topAmenities ?? [],
    pricePerNight: room.pricePerNight,
    nights,
    // Tổng CẢ KỲ do backend tính. Frontend nhân giá × số đêm sẽ lệch ngay khi
    // có mã giảm giá hoặc làm tròn.
    totalPrice: room.totalPrice,
    availableCount: room.availableCount,
  };
}

/*
 * KHÔNG có hàm ánh xạ cho danh mục tĩnh (khi khách chưa chọn ngày), và đó là
 * chủ ý. `ui-room-card` coi `availableCount <= 0` là HẾT PHÒNG: nó đổi nhãn nút
 * thành "Hết phòng" và vô hiệu hoá nút đó. Chưa chọn ngày thì chưa ai biết còn
 * bao nhiêu phòng, nên mọi con số truyền vào đều là nói dối — số 0 nói dối theo
 * hướng đuổi khách đi, còn một số lớn bịa ra nói dối theo hướng ngược lại.
 *
 * Trang /phong khi chưa có ngày vì thế hiển thị thẻ danh mục đơn giản (ảnh,
 * tên, giá "từ X mỗi đêm") dẫn sang trang chi tiết, và chỉ đổi sang
 * `ui-room-card` khi đã có ngày để hỏi phòng trống.
 */
