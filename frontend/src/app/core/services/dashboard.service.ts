import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { DashboardSummary } from './admin.types';

/** Số liệu tổng hợp của trang chủ khu quản trị. */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /**
   * @param from ngày đầu kỳ (bao gồm), `to` là ngày cuối kỳ (KHÔNG bao gồm).
   *   Truyền cả hai khi cần đối chiếu với truy vấn SQL thô — hai bên chỉ khớp
   *   khi dùng cùng tham số kỳ.
   */
  summary(from?: string | null, to?: string | null): Observable<DashboardSummary> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<DashboardSummary>('/api/admin/dashboard', { params });
  }
}
