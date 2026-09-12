import { Routes } from '@angular/router';
import { adminGuard } from '../../core/guards/admin.guard';
import { AdminLayout } from '../../layouts/admin-layout/admin-layout';

/**
 * Định tuyến khu quản trị.
 *
 * `/admin/login` nằm NGOÀI guard: đặt nó bên trong sẽ tạo vòng lặp chuyển
 * hướng — guard đẩy người chưa đăng nhập tới trang đăng nhập, mà trang đăng
 * nhập lại nằm sau chính guard đó.
 */
export const adminRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/admin-login').then((m) => m.AdminLogin),
    title: 'Đăng nhập quản trị — Homestay TVH',
  },
  {
    path: '',
    component: AdminLayout,
    canActivate: [adminGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./dashboard/admin-dashboard').then((m) => m.AdminDashboard),
        title: 'Tổng quan — Quản trị',
      },
      {
        path: 'bookings',
        loadComponent: () => import('./bookings/admin-bookings').then((m) => m.AdminBookings),
        title: 'Đơn đặt phòng — Quản trị',
      },
      {
        path: 'bookings/:id',
        loadComponent: () =>
          import('./bookings/booking-detail.component').then((m) => m.BookingDetailComponent),
        title: 'Chi tiết đơn — Quản trị',
      },
      {
        path: 'payments',
        loadComponent: () => import('./payments/admin-payments').then((m) => m.AdminPayments),
        title: 'Đối soát thanh toán — Quản trị',
      },
      {
        path: 'room-types',
        loadComponent: () => import('./room-types/admin-room-types').then((m) => m.AdminRoomTypes),
        title: 'Loại phòng — Quản trị',
      },
      {
        path: 'rooms',
        loadComponent: () => import('./rooms/admin-rooms').then((m) => m.AdminRooms),
        title: 'Phòng — Quản trị',
      },
      {
        path: 'promotions',
        loadComponent: () => import('./promotions/admin-promotions').then((m) => m.AdminPromotions),
        title: 'Khuyến mãi — Quản trị',
      },
      {
        path: 'reviews',
        loadComponent: () => import('./reviews/admin-reviews').then((m) => m.AdminReviews),
        title: 'Đánh giá — Quản trị',
      },
      {
        path: 'content/sections',
        loadComponent: () => import('./content/admin-sections').then((m) => m.AdminSections),
        title: 'Nội dung trang chủ — Quản trị',
      },
      {
        path: 'content/banners',
        loadComponent: () => import('./content/admin-banners').then((m) => m.AdminBanners),
        title: 'Banner — Quản trị',
      },
      {
        path: 'content/gallery',
        loadComponent: () => import('./content/admin-gallery').then((m) => m.AdminGallery),
        title: 'Thư viện ảnh — Quản trị',
      },
      {
        path: 'content/posts',
        loadComponent: () => import('./content/admin-posts').then((m) => m.AdminPosts),
        title: 'Tin tức — Quản trị',
      },
    ],
  },
];
