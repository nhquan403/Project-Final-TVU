import { Pipe, PipeTransform } from '@angular/core';

/**
 * Định dạng tiền Việt: `1250000` → `1.250.000 ₫`.
 *
 * Dùng `vi-VN` với `maximumFractionDigits: 0` — VND không có đơn vị nhỏ hơn
 * đồng, hiện `,00` chỉ làm giá dài ra và khó liếc.
 */
@Pipe({ name: 'vndCurrency' })
export class VndCurrencyPipe implements PipeTransform {
  private static readonly formatter = new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  });

  transform(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '—';
    }
    return VndCurrencyPipe.formatter.format(value);
  }
}
