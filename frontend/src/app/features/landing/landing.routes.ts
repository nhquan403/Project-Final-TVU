import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { PublicLayout } from '../../layouts/public-layout/public-layout';

/**
 * Định tuyến khu công khai.
 *
 * <p>Đường dẫn viết bằng tiếng Việt không dấu vì người dùng cuối là khách
 * trong nước, và vì chúng đã bị "đóng đinh" từ các phase trước: `auth.guard`
 * chuyển hướng tới `/dang-nhap`, thư xác nhận của Phase 6 gửi link
 * `/dat-phong/hoan-tat/{code}`. Đổi tên đường dẫn bây giờ là làm chết những
 * link đã gửi đi.
 */
export const landingRoutes: Routes = [
  {
    path: '',
    component: PublicLayout,
    children: [
      {
        path: '',
        loadComponent: () => import('./home/home').then((m) => m.HomePage),
        title: 'Homestay TVH — Đặt phòng trực tuyến',
      },
      {
        path: 'phong',
        loadComponent: () => import('./rooms/room-list').then((m) => m.RoomListPage),
        title: 'Các loại phòng — Homestay TVH',
      },
      {
        path: 'phong/:slug',
        loadComponent: () => import('./rooms/room-detail').then((m) => m.RoomDetailPage),
      },
      {
        path: 'dat-phong',
        loadComponent: () => import('./booking/booking-flow').then((m) => m.BookingFlowPage),
        title: 'Đặt phòng — Homestay TVH',
      },
      {
        path: 'dat-phong/thanh-toan/:code',
        loadComponent: () => import('./booking/payment-page').then((m) => m.PaymentPage),
        title: 'Thanh toán giữ chỗ — Homestay TVH',
      },
      {
        path: 'dat-phong/hoan-tat/:code',
        loadComponent: () => import('./booking/booking-complete').then((m) => m.BookingCompletePage),
        title: 'Đặt phòng thành công — Homestay TVH',
      },
      {
        path: 'tra-cuu',
        loadComponent: () => import('./lookup/lookup').then((m) => m.LookupPage),
        title: 'Tra cứu đơn đặt phòng — Homestay TVH',
      },
      {
        path: 'tai-khoan/dat-phong',
        canActivate: [authGuard],
        loadComponent: () => import('./account/my-bookings').then((m) => m.MyBookingsPage),
        title: 'Đơn của tôi — Homestay TVH',
      },
      {
        path: 'danh-gia/:code',
        loadComponent: () => import('./review/review-form').then((m) => m.ReviewFormPage),
        title: 'Viết đánh giá — Homestay TVH',
      },
      {
        path: 'tin-tuc',
        loadComponent: () => import('./news/news-list').then((m) => m.NewsListPage),
        title: 'Tin tức — Homestay TVH',
      },
      {
        path: 'tin-tuc/:slug',
        loadComponent: () => import('./news/news-detail').then((m) => m.NewsDetailPage),
      },
      {
        path: 'dang-nhap',
        loadComponent: () => import('./auth/login').then((m) => m.LoginPage),
        title: 'Đăng nhập — Homestay TVH',
      },
      {
        // Trang kiểm tra kết nối của Phase 1. Giữ lại nhưng chuyển khỏi `''`:
        // nó là công cụ chẩn đoán — hữu ích khi đứng trước hội đồng mà API im
        // lặng — chứ không phải trang bán hàng. Không nằm trong thanh điều
        // hướng, chỉ ai biết đường dẫn mới vào.
        path: 'trang-thai-he-thong',
        loadComponent: () => import('../health/health').then((m) => m.HealthPage),
        title: 'Trạng thái hệ thống — Homestay TVH',
      },
    ],
  },
];
