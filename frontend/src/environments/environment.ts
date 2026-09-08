/**
 * Cấu hình mặc định — dùng cho `ng serve` và cho bản dựng `demo`.
 * Bản `production` thay file này bằng environment.prod.ts (xem angular.json).
 */
export const environment = {
  production: false,
  /** Trang /ui-kit chỉ tồn tại ở bản demo, không có trong bản production. */
  uiKitEnabled: true,
};
