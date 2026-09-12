import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface SectionView {
  key: string;
  title: string | null;
  subtitle: string | null;
  /**
   * HTML ĐÃ được backend lọc ở tầng vào, nên render bằng `[innerHTML]` là an
   * toàn. Đây là trường DUY NHẤT của lớp nội dung được phép làm vậy.
   */
  body: string | null;
  imageUrl: string | null;
  updatedAt: string | null;
}

export interface BannerView {
  id: number;
  /** Văn bản thuần — hiển thị bằng text binding. */
  title: string;
  imageUrl: string;
  publicId: string | null;
  linkUrl: string | null;
  displayOrder: number;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
}

export interface GalleryImageView {
  id: number;
  url: string;
  publicId: string | null;
  /** Văn bản thuần — hiển thị bằng text binding. */
  caption: string | null;
  category: string | null;
  displayOrder: number;
  active: boolean;
}

export interface PostSummary {
  id: number;
  slug: string;
  title: string;
  excerpt: string | null;
  coverImageUrl: string | null;
  published: boolean;
  publishedAt: string | null;
}

export interface PostDetail extends PostSummary {
  /** HTML đã lọc ở tầng vào. */
  content: string | null;
}

export interface HomeContent {
  sections: SectionView[];
  banners: BannerView[];
  gallery: GalleryImageView[];
  latestPosts: PostSummary[];
}

/** Nội dung do admin soạn, hiển thị trên trang công khai. */
@Injectable({ providedIn: 'root' })
export class ContentService {
  private readonly http = inject(HttpClient);

  /** Một lượt gọi cho cả trang chủ, để trang không nhấp nháy từng khối. */
  home(): Observable<HomeContent> {
    return this.http.get<HomeContent>('/api/content/home');
  }

  posts(): Observable<PostSummary[]> {
    return this.http.get<PostSummary[]>('/api/posts');
  }

  post(slug: string): Observable<PostDetail> {
    return this.http.get<PostDetail>(`/api/posts/${encodeURIComponent(slug)}`);
  }
}
