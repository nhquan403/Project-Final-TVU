import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

/** Thẻ mô tả một trang cho công cụ tìm kiếm và khung xem trước khi chia sẻ. */
export interface PageSeo {
  title: string;
  description: string;
  /** Ảnh đại diện khi dán link vào Facebook/Zalo. Bỏ trống thì không đặt thẻ. */
  imageUrl?: string | null;
  type?: 'website' | 'article';
}

const SUFFIX = 'Homestay TVH';

/**
 * Đặt tiêu đề và thẻ mô tả cho những trang có nội dung động.
 *
 * <p>Các trang tĩnh khai `title` ngay trong bảng định tuyến — Angular tự đặt.
 * Nhưng trang chi tiết phòng và bài viết chỉ biết tiêu đề của mình SAU khi API
 * trả về, nên chúng cần đặt muộn. Không có lớp này thì mọi trang phòng đều
 * chung một tiêu đề, và link chia sẻ nào cũng hiện cùng một dòng chữ.
 *
 * <p>Đây là SEO ở mức ứng dụng một trang: thẻ được đặt sau khi JavaScript
 * chạy, nên chỉ những trình thu thập biết chạy JavaScript mới đọc được. Kết
 * xuất phía máy chủ nằm ngoài phạm vi đồ án; ghi rõ ra đây để không ai tưởng
 * lầm là đã có.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);

  apply(seo: PageSeo): void {
    const fullTitle = seo.title.includes(SUFFIX) ? seo.title : `${seo.title} — ${SUFFIX}`;
    this.title.setTitle(fullTitle);
    this.meta.updateTag({ name: 'description', content: seo.description });
    this.meta.updateTag({ property: 'og:title', content: fullTitle });
    this.meta.updateTag({ property: 'og:description', content: seo.description });
    this.meta.updateTag({ property: 'og:type', content: seo.type ?? 'website' });
    this.meta.updateTag({ property: 'og:url', content: this.document.location.href });

    if (seo.imageUrl) {
      this.meta.updateTag({ property: 'og:image', content: this.absolute(seo.imageUrl) });
    } else {
      // Gỡ ảnh của trang TRƯỚC đó. Ứng dụng một trang không tải lại tài liệu,
      // nên thẻ cũ ở lại và bài viết không ảnh sẽ mượn ảnh của bài vừa xem.
      this.meta.removeTag("property='og:image'");
    }
  }

  private absolute(url: string): string {
    return url.startsWith('http') ? url : new URL(url, this.document.location.origin).toString();
  }
}
