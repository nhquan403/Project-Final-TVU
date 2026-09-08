/**
 * Tính tỉ lệ tương phản theo công thức WCAG 2.x.
 *
 * Dùng cho trang `/ui-kit`: bảng màu ở đó đo tương phản TẠI THỜI ĐIỂM CHẠY từ
 * giá trị token thật, không đọc lại con số viết sẵn trong kế hoạch. Ai đổi
 * token mà làm tụt dưới ngưỡng AA là thấy ngay trên màn hình, không phải đợi
 * ai đó nhớ ra phải tính lại.
 */

/** Ngưỡng WCAG. */
export const AA_TEXT = 4.5;
export const AA_LARGE_TEXT = 3;
/** WCAG 1.4.11: ranh giới của thành phần tương tác. */
export const AA_NON_TEXT = 3;

/** Đọc giá trị một biến CSS từ `:root`, đã chuẩn hoá khoảng trắng. */
export function readToken(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

/** `#RGB` hoặc `#RRGGBB` → [r, g, b] trong khoảng 0–255. */
export function parseHex(hex: string): [number, number, number] | null {
  const value = hex.trim().replace('#', '');
  const full =
    value.length === 3
      ? value
          .split('')
          .map((c) => c + c)
          .join('')
      : value;
  if (!/^[0-9a-fA-F]{6}$/.test(full)) {
    return null;
  }
  return [
    parseInt(full.slice(0, 2), 16),
    parseInt(full.slice(2, 4), 16),
    parseInt(full.slice(4, 6), 16),
  ];
}

/** Độ chói tương đối theo WCAG. */
export function relativeLuminance([r, g, b]: [number, number, number]): number {
  const channel = (value: number): number => {
    const srgb = value / 255;
    return srgb <= 0.03928 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

/**
 * Tỉ lệ tương phản giữa hai màu hex. Trả `null` nếu không phân tích được màu —
 * gọi nơi dùng phải hiển thị rõ "không đo được" thay vì im lặng cho qua.
 */
export function contrastRatio(foreground: string, background: string): number | null {
  const fg = parseHex(foreground);
  const bg = parseHex(background);
  if (!fg || !bg) {
    return null;
  }
  const lighter = Math.max(relativeLuminance(fg), relativeLuminance(bg));
  const darker = Math.min(relativeLuminance(fg), relativeLuminance(bg));
  return (lighter + 0.05) / (darker + 0.05);
}

/** `7.532…` → `7.53:1`. */
export function formatRatio(ratio: number | null): string {
  return ratio === null ? 'không đo được' : `${ratio.toFixed(2)}:1`;
}
