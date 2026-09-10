import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import type { AvailabilityMap, RoomCardData } from '../../shared/ui';

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
   * Lịch giá và số phòng trống theo từng ngày, đưa thẳng vào
   * `ui-date-range-picker` để nó chặn sẵn ngày hết phòng.
   *
   * <p>API trả ĐỦ mọi ngày trong khoảng, kể cả ngày `availableCount: 0`. Điều
   * đó là bắt buộc: lịch coi ngày vắng mặt trong bản đồ là ngày KHÔNG bị chặn,
   * nên một ngày hết phòng bị bỏ sót sẽ cho khách chọn đúng ngày không đặt được.
   */
  calendar(roomTypeId: number, from: string, to: string): Observable<AvailabilityMap> {
    const params = new HttpParams().set('roomTypeId', roomTypeId).set('from', from).set('to', to);
    return this.http
      .get<DayAvailabilityResponse>('/api/availability/calendar', { params })
      .pipe(map((response) => response.days));
  }

  /** Ghép kết quả tìm kiếm vào hình dạng mà `ui-room-card` nhận. */
  static toRoomCard(item: RoomTypeAvailability): RoomCardData {
    return {
      id: String(item.roomTypeId),
      name: item.name,
      imageUrl: item.coverImageUrl ?? '',
      imageAlt: `Ảnh ${item.name}`,
      capacity: item.capacityAdults + item.capacityChildren,
      bedSummary: item.bedInfo ?? '',
      topAmenities: item.topAmenities,
      pricePerNight: item.pricePerNight,
      nights: item.nights,
      totalPrice: item.totalPrice,
      availableCount: item.availableCount,
    };
  }
}
