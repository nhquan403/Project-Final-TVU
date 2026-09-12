import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type {
  BannerView,
  GalleryImageView,
  PostDetail,
  PostSummary,
  SectionView,
} from './content.service';

export interface SectionRequest {
  title: string | null;
  subtitle: string | null;
  /** HTML — backend lọc ở tầng vào trước khi lưu. */
  body: string | null;
  imageUrl: string | null;
}

export interface BannerRequest {
  title: string;
  imageUrl: string;
  publicId: string | null;
  /** Chỉ http/https/mailto hoặc đường dẫn tương đối; backend chặn `javascript:`. */
  linkUrl: string | null;
  displayOrder: number;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
}

export interface GalleryImageRequest {
  url: string;
  publicId: string | null;
  caption: string | null;
  category: string | null;
  displayOrder: number;
  active: boolean;
}

export interface PostRequest {
  slug: string;
  title: string;
  excerpt: string | null;
  content: string | null;
  coverImageUrl: string | null;
  published: boolean;
}

/** Bốn nhóm nội dung do quản trị viên soạn. */
@Injectable({ providedIn: 'root' })
export class AdminContentService {
  private readonly http = inject(HttpClient);

  sections(): Observable<SectionView[]> {
    return this.http.get<SectionView[]>('/api/admin/content/sections');
  }

  saveSection(key: string, body: SectionRequest): Observable<SectionView> {
    return this.http.put<SectionView>(
      `/api/admin/content/sections/${encodeURIComponent(key)}`,
      body,
    );
  }

  banners(): Observable<BannerView[]> {
    return this.http.get<BannerView[]>('/api/admin/content/banners');
  }

  createBanner(body: BannerRequest): Observable<BannerView> {
    return this.http.post<BannerView>('/api/admin/content/banners', body);
  }

  updateBanner(id: number, body: BannerRequest): Observable<BannerView> {
    return this.http.put<BannerView>(`/api/admin/content/banners/${id}`, body);
  }

  deleteBanner(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/content/banners/${id}`);
  }

  gallery(): Observable<GalleryImageView[]> {
    return this.http.get<GalleryImageView[]>('/api/admin/content/gallery');
  }

  createGalleryImage(body: GalleryImageRequest): Observable<GalleryImageView> {
    return this.http.post<GalleryImageView>('/api/admin/content/gallery', body);
  }

  updateGalleryImage(id: number, body: GalleryImageRequest): Observable<GalleryImageView> {
    return this.http.put<GalleryImageView>(`/api/admin/content/gallery/${id}`, body);
  }

  deleteGalleryImage(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/content/gallery/${id}`);
  }

  posts(): Observable<PostSummary[]> {
    return this.http.get<PostSummary[]>('/api/admin/content/posts');
  }

  post(id: number): Observable<PostDetail> {
    return this.http.get<PostDetail>(`/api/admin/content/posts/${id}`);
  }

  createPost(body: PostRequest): Observable<PostDetail> {
    return this.http.post<PostDetail>('/api/admin/content/posts', body);
  }

  updatePost(id: number, body: PostRequest): Observable<PostDetail> {
    return this.http.put<PostDetail>(`/api/admin/content/posts/${id}`, body);
  }

  deletePost(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/content/posts/${id}`);
  }
}
