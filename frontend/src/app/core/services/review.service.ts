import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface PublicReview {
  id: number;
  guestName: string;
  rating: number;
  /** VĂN BẢN THUẦN do người ẩn danh gửi. Hiển thị bằng text binding, không innerHTML. */
  title: string | null;
  content: string | null;
  roomTypeName: string;
  adminReply: string | null;
  createdAt: string;
}

export interface SubmitReviewInput {
  /** Một trong hai: mã truy cập của đơn, hoặc số điện thoại đã đặt. */
  token?: string | null;
  phone?: string | null;
  rating: number;
  title?: string | null;
  content?: string | null;
}

/** Đánh giá của khách sau khi trả phòng. */
@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);

  /** Chỉ trả đánh giá ĐÃ được duyệt — bộ lọc nằm ở backend, không ở đây. */
  published(): Observable<PublicReview[]> {
    return this.http.get<PublicReview[]>('/api/reviews');
  }

  submit(code: string, input: SubmitReviewInput): Observable<PublicReview> {
    return this.http.post<PublicReview>(
      `/api/bookings/${encodeURIComponent(code)}/review`,
      input,
    );
  }
}
