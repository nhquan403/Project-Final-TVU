import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import type { ChartPoint } from './bar-chart';

const WIDTH = 720;
const HEIGHT = 220;
const PADDING = 12;
const PADDING_BOTTOM = 28;

/**
 * Biểu đồ đường vẽ tay bằng SVG, dùng cho tỉ lệ lấp đầy.
 *
 * Trục tung cố định 0–100%: tỉ lệ lấp đầy là một phân số có trần tự nhiên, và
 * tự co trục theo giá trị lớn nhất sẽ khiến một tháng 8% trông như tháng kín
 * phòng. Mỗi điểm vẫn là một phần tử riêng có nhãn đọc được.
 */
@Component({
  selector: 'app-line-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <figure class="text-focus">
      <figcaption class="mb-1 text-sm font-semibold text-text">{{ title() }}</figcaption>
      <p class="mb-2 text-xs text-text-muted">{{ axisNote() }}</p>

      @if (points().length === 0) {
        <p class="text-sm text-text-muted">Chưa có dữ liệu trong kỳ này.</p>
      } @else {
        <svg
          [attr.viewBox]="'0 0 ' + width + ' ' + height"
          class="h-56 w-full"
          role="img"
          [attr.aria-label]="title() + '. ' + summary()">
          @for (line of gridLines(); track line.value) {
            <g>
              <line
                [attr.x1]="0"
                [attr.y1]="line.y"
                [attr.x2]="width"
                [attr.y2]="line.y"
                stroke="currentColor"
                stroke-width="1"
                opacity="0.15" />
              <text [attr.x]="0" [attr.y]="line.y - 3" class="fill-text-muted" font-size="10">
                {{ line.value }}%
              </text>
            </g>
          }

          <polyline
            [attr.points]="polyline()"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linejoin="round" />

          @for (dot of dots(); track dot.label) {
            <g>
              <circle [attr.cx]="dot.x" [attr.cy]="dot.y" r="3" fill="currentColor">
                <title>{{ dot.label }}: {{ dot.display }}</title>
              </circle>
              <text
                [attr.x]="dot.x"
                [attr.y]="height - 10"
                text-anchor="middle"
                class="fill-text-muted"
                font-size="11">
                {{ dot.label }}
              </text>
            </g>
          }
        </svg>
      }
    </figure>
  `,
})
export class LineChart {
  readonly title = input.required<string>();
  readonly axisNote = input('');
  /** `value` là tỉ lệ 0–1, không phải phần trăm. */
  readonly points = input<readonly ChartPoint[]>([]);

  protected readonly width = WIDTH;
  protected readonly height = HEIGHT;

  protected readonly gridLines = computed(() =>
    [0, 25, 50, 75, 100].map((value) => ({ value, y: this.yFor(value / 100) })),
  );

  protected readonly dots = computed(() => {
    const points = this.points();
    const step = points.length <= 1 ? 0 : (WIDTH - PADDING * 2) / (points.length - 1);
    return points.map((point, index) => ({
      label: point.label,
      display: point.display,
      x: PADDING + step * index,
      y: this.yFor(point.value),
    }));
  });

  protected readonly polyline = computed(() =>
    this.dots()
      .map((dot) => `${dot.x},${dot.y}`)
      .join(' '),
  );

  protected readonly summary = computed(() =>
    this.points()
      .map((point) => `${point.label}: ${point.display}`)
      .join('; '),
  );

  private yFor(ratio: number): number {
    const plotHeight = HEIGHT - PADDING_BOTTOM - PADDING;
    return PADDING + plotHeight * (1 - Math.min(Math.max(ratio, 0), 1));
  }
}
