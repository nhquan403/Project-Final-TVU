import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface PublicImage {
  url: string;
  altText: string;
  cover: boolean;
}

export interface PublicAmenity {
  code: string;
  name: string;
  icon: string | null;
  /** `ROOM` = tiện ích trong phòng, `PROPERTY` = tiện ích của cả homestay. */
  category: 'ROOM' | 'PROPERTY';
}

export interface PublicRoomType {
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
  amenities: PublicAmenity[];
  images: PublicImage[];
}

/** Danh mục loại phòng cho trang công khai. Chỉ loại đang bán. */
@Injectable({ providedIn: 'root' })
export class RoomTypeService {
  private readonly http = inject(HttpClient);

  list(): Observable<PublicRoomType[]> {
    return this.http.get<PublicRoomType[]>('/api/room-types');
  }

  bySlug(slug: string): Observable<PublicRoomType> {
    return this.http.get<PublicRoomType>(`/api/room-types/${encodeURIComponent(slug)}`);
  }
}
