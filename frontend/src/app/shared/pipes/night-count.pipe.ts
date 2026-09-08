import { Pipe, PipeTransform } from '@angular/core';

/**
 * Số đêm giữa hai ngày, đếm theo khoảng nửa mở `[nhận, trả)` — đúng quy ước
 * của ràng buộc `EXCLUDE` trên `daterange` ở tầng cơ sở dữ liệu. Nhận phòng
 * 10/3 và trả phòng 12/3 là **2 đêm**, không phải 3 ngày.
 *
 * Tính bằng UTC để một đêm chuyển mùa hè/mùa đông không biến thành 0 hay 2.
 */
@Pipe({ name: 'nightCount' })
export class NightCountPipe implements PipeTransform {
  private static readonly MS_PER_DAY = 24 * 60 * 60 * 1000;

  transform(checkIn: Date | string | null, checkOut: Date | string | null): number {
    if (!checkIn || !checkOut) {
      return 0;
    }
    const from = NightCountPipe.toUtcMidnight(checkIn);
    const to = NightCountPipe.toUtcMidnight(checkOut);
    if (from === null || to === null) {
      return 0;
    }
    return Math.max(0, Math.round((to - from) / NightCountPipe.MS_PER_DAY));
  }

  private static toUtcMidnight(value: Date | string): number | null {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
      return null;
    }
    return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
  }
}
