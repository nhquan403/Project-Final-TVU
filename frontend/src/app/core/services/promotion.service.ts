import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface CheckPromotionInput {
  code: string;
  roomTypeId: number;
  checkIn: string;
  checkOut: string;
  roomQuantity: number;
}

/** Kết quả thử mã. MỌI con số ở đây do backend tính. */
export interface CheckPromotionResult {
  code: string;
  name: string;
  description: string | null;
  nights: number;
  subtotalAmount: number;
  discountAmount: number;
  totalAmount: number;
  depositAmount: number;
}

/** Thử mã giảm giá trước khi đặt phòng. */
@Injectable({ providedIn: 'root' })
export class PromotionService {
  private readonly http = inject(HttpClient);

  /**
   * Không gửi số tiền lên: giá lấy từ loại phòng trong cơ sở dữ liệu, số đêm
   * tính từ ngày. Gửi subtotal từ client là để client tự khai mình đặt 100
   * triệu rồi nhận mã giảm 10% của con số đó.
   */
  check(input: CheckPromotionInput): Observable<CheckPromotionResult> {
    return this.http.post<CheckPromotionResult>('/api/promotions/check', input);
  }
}
