import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface ChartPoint {
  /** Nhãn trục hoành, ví dụ `03/2026`. */
  label: string;
  value: number;
  /** Giá trị đã định dạng để đọc, ví dụ `1.700.000 ₫`. */
  display: string;
}

const WIDTH = 720;
const HEIGHT = 240;
const PADDING_LEFT = 8;
const PADDING_BOTTOM = 28;

/**
 * Biểu đồ cột vẽ tay bằng SVG.
 *
 * Không dùng thư viện biểu đồ, và đây là lựa chọn có lý do chứ không phải tiết
 * kiệm: bảng màu mặc định của mọi thư viện là mã màu cứng, nên muốn hợp token
 * vẫn phải đọc token bằng `getComputedStyle` rồi nhồi ngược vào cấu hình —
 * nhiều việc hơn là vẽ hai loại biểu đồ đơn giản. Vẽ bằng SVG còn cho mỗi cột
 * là một phần tử DOM thật, nên nhãn cho trình đọc màn hình là chuyện hiển
 * nhiên; với `<canvas>` thì phải dựng thêm một bảng ẩn song song.
 *
 * Màu lấy qua `currentColor` từ lớp bọc ngoài, nên không có mã màu nào trong
 * mã nguồn này.
 */
@Component({
  selector: 'app-bar-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <figure class="text-primary">
      <figcaption class="mb-1 text-sm font-semibold text-text">{{ title() }}</figcaption>
      <p class="mb-2 text-xs text-text-muted">{{ axisNote() }}</p>

      @if (points().length === 0) {
        <p class="text-sm text-text-muted">Chưa có dữ liệu trong kỳ này.</p>
      } @else {
        <svg
          [attr.viewBox]="'0 0 ' + width + ' ' + height"
          class="h-60 w-full"
          role="img"
          [attr.aria-label]="title() + '. ' + summary()">
          <!-- Đường đáy -->
          <line
            [attr.x1]="0"
            [attr.y1]="height - paddingBottom"
            [attr.x2]="width"
            [attr.y2]="height - paddingBottom"
            stroke="currentColor"
            stroke-width="1"
            opacity="0.25" />

          @for (bar of bars(); track bar.label) {
            <g>
              <!-- Mỗi cột là một phần tử thật, có nhãn đọc được riêng. -->
              <rect
                [attr.x]="bar.x"
                [attr.y]="bar.y"
                [attr.width]="bar.width"
                [attr.height]="bar.height"
                fill="currentColor"
                rx="2">
                <title>{{ bar.label }}: {{ bar.display }}</title>
              </rect>
              <text
                [attr.x]="bar.x + bar.width / 2"
                [attr.y]="height - 10"
                text-anchor="middle"
                class="fill-text-muted"
                font-size="11">
                {{ bar.label }}
              </text>
            </g>
          }
        </svg>
      }
    </figure>
  `,
})
export class BarChart {
  readonly title = input.required<string>();
  /** Câu nói rõ trục thời gian. Hai con số cạnh nhau mà thiếu câu này sẽ bị hiểu là cùng kỳ. */
  readonly axisNote = input('');
  readonly points = input<readonly ChartPoint[]>([]);

  protected readonly width = WIDTH;
  protected readonly height = HEIGHT;
  protected readonly paddingBottom = PADDING_BOTTOM;

  protected readonly bars = computed(() => {
    const points = this.points();
    if (points.length === 0) {
      return [];
    }
    const max = Math.max(...points.map((point) => point.value), 0);
    const slot = (WIDTH - PADDING_LEFT * 2) / points.length;
    const barWidth = Math.max(slot * 0.6, 2);
    const plotHeight = HEIGHT - PADDING_BOTTOM - 8;

    return points.map((point, index) => {
      // max = 0 nghĩa là cả kỳ không có gì: vẽ cột cao 0 thay vì chia cho 0.
      const height = max === 0 ? 0 : (point.value / max) * plotHeight;
      return {
        label: point.label,
        display: point.display,
        x: PADDING_LEFT + slot * index + (slot - barWidth) / 2,
        y: HEIGHT - PADDING_BOTTOM - height,
        width: barWidth,
        height,
      };
    });
  });

  /** Tóm tắt bằng lời cho trình đọc màn hình — thứ thay thế cho việc nhìn biểu đồ. */
  protected readonly summary = computed(() =>
    this.points()
      .map((point) => `${point.label}: ${point.display}`)
      .join('; '),
  );
}
