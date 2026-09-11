import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  AdminBookingDetail,
  AdminBookingRow,
  OutboundEmailView,
  Page,
  ReconcileRow,
} from './admin.types';

/** Bộ lọc của bảng đơn. Mọi trường đều tuỳ chọn và được đồng bộ lên query param. */
export interface BookingFilter {
  status?: string | null;
  from?: string | null;
  to?: string | null;
  q?: string | null;
  page?: number;
  size?: number;
}

/** Đơn và hàng đợi đối soát ở khu quản trị. */
@Injectable({ providedIn: 'root' })
export class AdminBookingService {
  private readonly http = inject(HttpClient);

  list(filter: BookingFilter): Observable<Page<AdminBookingRow>> {
    let params = new HttpParams()
      .set('page', filter.page ?? 0)
      .set('size', filter.size ?? 20);
    // Chỉ gửi tham số có giá trị: `status=` rỗng trên URL khiến đường dẫn đã
    // lọc trông khác nhau tuỳ cách người dùng tới, và bookmark hết giống nhau.
    if (filter.status) params = params.set('status', filter.status);
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    if (filter.q) params = params.set('q', filter.q);
    return this.http.get<Page<AdminBookingRow>>('/api/admin/bookings', { params });
  }

  detail(id: number): Observable<AdminBookingDetail> {
    return this.http.get<AdminBookingDetail>(`/api/admin/bookings/${id}`);
  }

  transition(id: number, toStatus: string, note?: string): Observable<AdminBookingDetail> {
    return this.http.post<AdminBookingDetail>(`/api/admin/bookings/${id}/transition`, {
      toStatus,
      note: note ?? null,
    });
  }

  addNote(id: number, content: string): Observable<AdminBookingDetail> {
    return this.http.post<AdminBookingDetail>(`/api/admin/bookings/${id}/note`, { content });
  }

  resendEmail(id: number, template?: string): Observable<OutboundEmailView> {
    return this.http.post<OutboundEmailView>(`/api/admin/bookings/${id}/resend-email`, {
      template: template ?? null,
    });
  }

  /** Đường tải CSV. Trình duyệt tự tải; interceptor gắn token vào request này như mọi request khác. */
  exportCsvUrl(filter: BookingFilter): string {
    const params = new URLSearchParams();
    if (filter.status) params.set('status', filter.status);
    if (filter.from) params.set('from', filter.from);
    if (filter.to) params.set('to', filter.to);
    const query = params.toString();
    return '/api/admin/reports/bookings.csv' + (query ? `?${query}` : '');
  }

  exportCsv(filter: BookingFilter): Observable<Blob> {
    return this.http.get(this.exportCsvUrl(filter), { responseType: 'blob' });
  }

  reconcileQueue(reconcileStatus?: string | null): Observable<Page<ReconcileRow>> {
    let params = new HttpParams().set('size', 50);
    if (reconcileStatus) params = params.set('reconcileStatus', reconcileStatus);
    return this.http.get<Page<ReconcileRow>>('/api/admin/payments', { params });
  }

  resolvePayment(paymentId: number, note?: string): Observable<ReconcileRow> {
    return this.http.post<ReconcileRow>(`/api/admin/payments/${paymentId}/resolve`, {
      note: note ?? null,
    });
  }

  confirmPaymentManually(paymentId: number, note?: string): Observable<ReconcileRow> {
    return this.http.post<ReconcileRow>(`/api/admin/payments/${paymentId}/confirm-manually`, {
      note: note ?? null,
    });
  }
}
