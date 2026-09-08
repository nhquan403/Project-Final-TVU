import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

/** Hình dạng phản hồi của GET /api/health. */
interface HealthResponse {
  status: string;
  time: string;
  appZone: string;
  jvmDefaultZone: string;
}

/**
 * Trang tạm của Phase 1: gọi /api/health qua proxy để chứng minh frontend và
 * backend đã nối được với nhau, và múi giờ phía server đặt đúng.
 * Phase 8 thay bằng landing page thật.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly http = inject(HttpClient);

  protected readonly health = signal<HealthResponse | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // Đường dẫn tương đối, đi qua proxy của ng serve sang localhost:8080.
    this.http.get<HealthResponse>('/api/health').subscribe({
      next: (response) => this.health.set(response),
      error: (err) => this.error.set(err?.message ?? 'Không gọi được API'),
    });
  }
}
