import { readToken } from '../../shared/ui';
import type { AvailabilityMap, LightboxImage, RoomCardData, SelectOption } from '../../shared/ui';

/**
 * Dữ liệu mẫu cho trang trình bày. Ảnh là SVG nội tuyến (data URI) chứ không
 * gọi ra internet: trang /ui-kit phải mở được ở buổi bảo vệ ngay cả khi phòng
 * hội đồng không có mạng.
 *
 * Màu nền của ảnh đọc từ token lúc chạy, không viết mã hex vào đây — mã hex
 * rải rác chính là thứ lint chặn màu được dựng lên để ngăn.
 */
function placeholderImage(label: string, token: string): string {
  const background = readToken(token) || 'gray';
  const foreground = readToken('--color-text-invert') || 'white';
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="420">
    <rect width="640" height="420" fill="${background}"/>
    <text x="320" y="220" font-family="sans-serif" font-size="28" fill="${foreground}"
          text-anchor="middle">${label}</text>
  </svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

export const SAMPLE_IMAGE = placeholderImage('Phòng Vườn', '--color-primary');

export const SAMPLE_ROOM: RoomCardData = {
  id: 'garden-double',
  name: 'Phòng Vườn — giường đôi',
  imageUrl: SAMPLE_IMAGE,
  imageAlt: 'Ảnh minh hoạ phòng vườn giường đôi',
  capacity: 2,
  bedSummary: '1 giường đôi',
  topAmenities: ['Ban công', 'Điều hoà', 'Bữa sáng'],
  pricePerNight: 850_000,
  nights: 2,
  totalPrice: 1_700_000,
  availableCount: 2,
};

export const SOLD_OUT_ROOM: RoomCardData = {
  ...SAMPLE_ROOM,
  id: 'garden-double-sold-out',
  name: 'Phòng Vườn — giường đôi (hết phòng)',
  availableCount: 0,
};

export const SAMPLE_OPTIONS: readonly SelectOption[] = [
  { value: 'garden', label: 'Phòng Vườn' },
  { value: 'river', label: 'Phòng Sông' },
  { value: 'family', label: 'Phòng Gia đình' },
  { value: 'penthouse', label: 'Penthouse (đang bảo trì)', disabled: true },
];

export const SAMPLE_IMAGES: readonly LightboxImage[] = [
  { url: SAMPLE_IMAGE, alt: 'Toàn cảnh phòng vườn' },
  { url: placeholderImage('Ban công', '--color-price'), alt: 'Ban công nhìn ra vườn' },
  { url: placeholderImage('Phòng tắm', '--color-text-muted'), alt: 'Phòng tắm' },
];

export interface DemoBooking {
  code: string;
  guest: string;
  nights: number;
  total: number;
}

export const SAMPLE_BOOKINGS: readonly DemoBooking[] = [
  { code: 'TVH-000124', guest: 'Nguyễn Văn A', nights: 2, total: 1_700_000 },
  { code: 'TVH-000125', guest: 'Trần Thị B', nights: 3, total: 2_550_000 },
  { code: 'TVH-000126', guest: 'Lê Văn C', nights: 1, total: 850_000 },
];

/**
 * Lịch giá mẫu: 60 ngày kể từ hôm nay. Ngày thứ 5, 6 và 7 hết phòng để trang
 * /ui-kit chứng minh được việc CHẶN SẴN ngày hết phòng, không phải hứa suông.
 */
export function buildSampleAvailability(): AvailabilityMap {
  const map: Record<string, { price: number; availableCount: number }> = {};
  const today = new Date();
  for (let offset = 0; offset < 60; offset++) {
    const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() + offset);
    const key = [
      date.getFullYear(),
      `${date.getMonth() + 1}`.padStart(2, '0'),
      `${date.getDate()}`.padStart(2, '0'),
    ].join('-');
    const weekend = date.getDay() === 5 || date.getDay() === 6;
    map[key] = {
      price: weekend ? 1_050_000 : 850_000,
      availableCount: offset >= 5 && offset <= 7 ? 0 : 3,
    };
  }
  return map;
}
