import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  AmenityView,
  PromotionView,
  ReviewView,
  RoomStatusResult,
  RoomTypeRequest,
  RoomTypeView,
  RoomView,
  UploadedImage,
} from './admin.types';

/** Danh mục: loại phòng, phòng vật lý, khuyến mãi, đánh giá, ảnh. */
@Injectable({ providedIn: 'root' })
export class AdminCatalogService {
  private readonly http = inject(HttpClient);

  // ─── Loại phòng ───────────────────────────────────────────────────────

  roomTypes(): Observable<RoomTypeView[]> {
    return this.http.get<RoomTypeView[]>('/api/admin/room-types');
  }

  createRoomType(body: RoomTypeRequest): Observable<RoomTypeView> {
    return this.http.post<RoomTypeView>('/api/admin/room-types', body);
  }

  updateRoomType(id: number, body: RoomTypeRequest): Observable<RoomTypeView> {
    return this.http.put<RoomTypeView>(`/api/admin/room-types/${id}`, body);
  }

  deleteRoomType(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/room-types/${id}`);
  }

  setAmenities(id: number, amenityIds: number[]): Observable<RoomTypeView> {
    return this.http.put<RoomTypeView>(`/api/admin/room-types/${id}/amenities`, { amenityIds });
  }

  addImage(id: number, url: string, publicId: string | null, altText: string): Observable<RoomTypeView> {
    return this.http.post<RoomTypeView>(`/api/admin/room-types/${id}/images`, {
      url,
      publicId,
      altText,
    });
  }

  reorderImages(
    id: number,
    images: { id: number; displayOrder: number; cover: boolean }[],
  ): Observable<RoomTypeView> {
    return this.http.put<RoomTypeView>(`/api/admin/room-types/${id}/images`, { images });
  }

  deleteImage(imageId: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/room-types/images/${imageId}`);
  }

  amenities(): Observable<AmenityView[]> {
    return this.http.get<AmenityView[]>('/api/admin/amenities');
  }

  /** Tải ảnh lên. Backend kiểm định dạng, giải mã lại và đổi tên thành UUID. */
  uploadImage(file: File, folder = 'room-types'): Observable<UploadedImage> {
    const form = new FormData();
    form.append('file', file);
    form.append('folder', folder);
    return this.http.post<UploadedImage>('/api/admin/images', form);
  }

  // ─── Phòng vật lý ─────────────────────────────────────────────────────

  rooms(roomTypeId?: number | null): Observable<RoomView[]> {
    let params = new HttpParams();
    if (roomTypeId) params = params.set('roomTypeId', roomTypeId);
    return this.http.get<RoomView[]>('/api/admin/rooms', { params });
  }

  createRoom(body: {
    roomTypeId: number;
    roomNumber: string;
    floor: number | null;
    note: string | null;
  }): Observable<RoomView> {
    return this.http.post<RoomView>('/api/admin/rooms', body);
  }

  updateRoom(
    id: number,
    body: { roomTypeId: number; roomNumber: string; floor: number | null; note: string | null },
  ): Observable<RoomView> {
    return this.http.put<RoomView>(`/api/admin/rooms/${id}`, body);
  }

  /** Phản hồi kèm danh sách đơn bị ảnh hưởng. Backend KHÔNG tự huỷ đơn nào. */
  changeRoomStatus(id: number, status: string): Observable<RoomStatusResult> {
    return this.http.patch<RoomStatusResult>(`/api/admin/rooms/${id}`, { status });
  }

  deleteRoom(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/rooms/${id}`);
  }

  // ─── Khuyến mãi ───────────────────────────────────────────────────────

  promotions(): Observable<PromotionView[]> {
    return this.http.get<PromotionView[]>('/api/admin/promotions');
  }

  createPromotion(body: Record<string, unknown>): Observable<PromotionView> {
    return this.http.post<PromotionView>('/api/admin/promotions', body);
  }

  updatePromotion(id: number, body: Record<string, unknown>): Observable<PromotionView> {
    return this.http.put<PromotionView>(`/api/admin/promotions/${id}`, body);
  }

  deletePromotion(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/promotions/${id}`);
  }

  // ─── Đánh giá ─────────────────────────────────────────────────────────

  reviews(status?: string | null): Observable<ReviewView[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<ReviewView[]>('/api/admin/reviews', { params });
  }

  approveReview(id: number): Observable<ReviewView> {
    return this.http.post<ReviewView>(`/api/admin/reviews/${id}/approve`, {});
  }

  rejectReview(id: number): Observable<ReviewView> {
    return this.http.post<ReviewView>(`/api/admin/reviews/${id}/reject`, {});
  }

  replyReview(id: number, reply: string): Observable<ReviewView> {
    return this.http.post<ReviewView>(`/api/admin/reviews/${id}/reply`, { reply });
  }
}
