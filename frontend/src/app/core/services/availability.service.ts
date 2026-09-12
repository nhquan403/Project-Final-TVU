import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import type { AvailabilityMap } from '../../shared/ui';

/** Một loại phòng còn trống, đúng hình dạng API trả về. */
export interface RoomTypeAvailability {
  roomTypeId: number;
  code: string;
  slug: string;
  name: string;
  shortDescription: string | null;
  bedInfo: string | null;
  capacityAdults: number;
  capacityChildren: number;
  pricePerNight: number;
  nights: number;
  totalPrice: number;
  availableCount: number;
  coverImageUrl: string | null;
  topAmenities: string[];
}

export interface AvailabilitySearchResponse {
  checkIn: string;
  checkOut: string;
  nights: number;
  roomTypes: RoomTypeAvailability[];
}

/** Hình dạng lịch giá do API trả về — khớp `AvailabilityMap` của ui-date-range-picker. */
interface DayAvailabilityResponse {
  days: AvailabilityMap;
}

/**
 * Tìm phòng trống và lấy lịch giá.
 *
 * <p>Không giữ trạng thái: các màn hình ở phase sau tự quyết định lưu kết quả
 * ở đâu. Ở đây chỉ có việc gọi API và chuyển đổi hình dạng dữ liệu.
 */
@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  private readonly http = inject(HttpClient);

  search(input: {
    checkIn: string;
    checkOut: string;
    adults: number;
    children?: number;
    roomQuantity?: number;
  }): Observable<AvailabilitySearchResponse> {
    const params = new HttpParams()
      .set('checkIn', input.checkIn)
      .set('checkOut', input.checkOut)
      .set('adults', input.adults)
      .set('children', input.children ?? 0)
      .set('roomQuantity', input.roomQuantity ?? 1);
    return this.http.get<AvailabilitySearchResponse>('/api/availability', { params });
  }

  /**
   * Bản đồ giá và số phòng trống theo từng ngày, đưa thẳng vào
   * `ui-date-range-picker` để nó chặn sẵn ngày hết phòng.
   *
   * <p>API trả ĐỦ mọi ngày trong khoảng, kể cả ngày `availableCount: 0`. Điều
   * đó là bắt buộc: lịch coi ngày vắng mặt trong bản đồ là ngày KHÔNG bị chặn,
   * nên một ngày hết phòng bị bỏ sót sẽ cho khách chọn đúng ngày không đặt được.
   *
   * <p>`roomTypeId = null` hỏi lịch của TOÀN homestay — thanh tìm phòng ở trang
   * chủ chạy trước khi khách chọn loại phòng, nên nó không có gì để truyền. Một
   * ngày chỉ bị chặn khi MỌI loại phòng đều hết chỗ. Tham số bị BỎ HẲN khỏi
   * chuỗi truy vấn trong trường hợp đó: `HttpParams.set` biến `null` thành chuỗi
   * "null", và Spring từ chối chuỗi đó khi ráp vào kiểu `Long`.
   */
  calendar(roomTypeId: number | null, from: string, to: string): Observable<AvailabilityMap> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (roomTypeId !== null) {
      params = params.set('roomTypeId', roomTypeId);
    }
    return this.http
      .get<DayAvailabilityResponse>('/api/availability/calendar', { params })
      .pipe(map((response) => response.days));
  }
}
