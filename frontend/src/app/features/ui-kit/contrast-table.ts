import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { AA_NON_TEXT, AA_TEXT, contrastRatio, formatRatio, readToken } from '../../shared/ui';

interface Pair {
  label: string;
  foreground: string;
  background: string;
  /** Ngưỡng phải đạt. `null` = trang trí, không chịu ràng buộc nào. */
  threshold: number | null;
  note?: string;
}

const PAIRS: readonly Pair[] = [
  { label: 'text trên bg', foreground: '--color-text', background: '--color-bg', threshold: AA_TEXT },
  { label: 'text trên surface', foreground: '--color-text', background: '--color-surface', threshold: AA_TEXT },
  { label: 'text-muted trên bg', foreground: '--color-text-muted', background: '--color-bg', threshold: AA_TEXT },
  { label: 'text-muted trên surface', foreground: '--color-text-muted', background: '--color-surface', threshold: AA_TEXT },
  { label: 'text trên surface-2', foreground: '--color-text', background: '--color-surface-2', threshold: AA_TEXT },
  { label: 'primary trên bg', foreground: '--color-primary', background: '--color-bg', threshold: AA_TEXT },
  { label: 'text-invert trên primary', foreground: '--color-text-invert', background: '--color-primary', threshold: AA_TEXT },
  { label: 'text-invert trên primary-hover', foreground: '--color-text-invert', background: '--color-primary-hover', threshold: AA_TEXT },
  { label: 'price trên bg', foreground: '--color-price', background: '--color-bg', threshold: AA_TEXT },
  { label: 'price trên surface', foreground: '--color-price', background: '--color-surface', threshold: AA_TEXT },
  { label: 'success trên bg', foreground: '--color-success', background: '--color-bg', threshold: AA_TEXT },
  { label: 'warning trên bg', foreground: '--color-warning', background: '--color-bg', threshold: AA_TEXT },
  { label: 'danger trên bg', foreground: '--color-danger', background: '--color-bg', threshold: AA_TEXT },
  { label: 'text-invert trên danger', foreground: '--color-text-invert', background: '--color-danger', threshold: AA_TEXT },
  { label: 'focus trên bg', foreground: '--color-focus', background: '--color-bg', threshold: AA_TEXT },
  {
    label: 'border-strong trên surface',
    foreground: '--color-border-strong',
    background: '--color-surface',
    threshold: AA_NON_TEXT,
    note: 'viền ô nhập và nút — WCAG 1.4.11',
  },
  {
    label: 'border-strong trên bg',
    foreground: '--color-border-strong',
    background: '--color-bg',
    threshold: AA_NON_TEXT,
    note: 'viền ô nhập và nút — WCAG 1.4.11',
  },
  {
    label: 'border trên bg',
    foreground: '--color-border',
    background: '--color-bg',
    threshold: null,
    note: 'vạch phân cách trang trí — không dùng cho viền ô nhập',
  },
];

/**
 * Bảng tương phản, đo TẠI THỜI ĐIỂM CHẠY từ giá trị token thật trong DOM.
 *
 * Không chép lại con số từ kế hoạch: một bảng số viết tay sẽ đúng đúng một
 * lần, rồi ai đó đổi mã hex và bảng thành lời nói dối. Đo lúc chạy thì đổi
 * token là thấy ngay ô nào tụt xuống dưới ngưỡng.
 */
@Component({
  selector: 'ui-kit-contrast-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="overflow-x-auto rounded-lg border border-border bg-surface">
      <table class="w-full border-collapse text-sm">
        <caption class="sr-only">Tỉ lệ tương phản của bảng màu, đo lúc chạy</caption>
        <thead>
          <tr class="border-b border-border bg-surface-2 text-left">
            <th scope="col" class="px-3 py-2 font-semibold">Cặp màu</th>
            <th scope="col" class="px-3 py-2 font-semibold">Mã màu</th>
            <th scope="col" class="px-3 py-2 text-right font-semibold">Đo được</th>
            <th scope="col" class="px-3 py-2 text-right font-semibold">Ngưỡng</th>
            <th scope="col" class="px-3 py-2 font-semibold">Kết quả</th>
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track row.label) {
            <tr class="border-b border-border">
              <td class="px-3 py-2">
                {{ row.label }}
                @if (row.note) {
                  <span class="block text-xs text-text-muted">{{ row.note }}</span>
                }
              </td>
              <td class="px-3 py-2">
                <span class="inline-flex items-center gap-2">
                  <span
                    class="inline-block h-4 w-4 rounded-sm border border-border-strong"
                    [style.background]="row.foregroundValue"
                    aria-hidden="true"></span>
                  <code class="text-xs">{{ row.foregroundValue || '—' }}</code>
                </span>
              </td>
              <td class="px-3 py-2 text-right tabular-nums">{{ row.ratioLabel }}</td>
              <td class="px-3 py-2 text-right tabular-nums">
                {{ row.threshold === null ? '—' : row.threshold + ':1' }}
              </td>
              <td class="px-3 py-2 font-semibold" [class]="row.toneClass">{{ row.verdict }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class UiKitContrastTable {
  /** Đổi giá trị này để buộc đo lại (ví dụ sau khi chỉnh token trong DevTools). */
  private readonly generation = signal(0);

  protected readonly rows = computed(() => {
    this.generation();
    return PAIRS.map((pair) => {
      const foregroundValue = readToken(pair.foreground);
      const backgroundValue = readToken(pair.background);
      const ratio = contrastRatio(foregroundValue, backgroundValue);
      const passed = pair.threshold === null || (ratio !== null && ratio >= pair.threshold);
      return {
        ...pair,
        foregroundValue,
        ratioLabel: formatRatio(ratio),
        verdict: pair.threshold === null ? 'trang trí' : passed ? 'đạt' : 'KHÔNG ĐẠT',
        toneClass: pair.threshold === null ? 'text-text-muted' : passed ? 'text-success' : 'text-danger',
      };
    });
  });
}
