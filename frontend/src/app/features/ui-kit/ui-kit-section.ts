import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Khung một mục trong trang /ui-kit: tiêu đề, ghi chú, rồi phần trình bày. */
@Component({
  selector: 'ui-kit-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="scroll-mt-20" [id]="anchor()">
      <h2 class="text-h2 font-semibold">{{ title() }}</h2>
      @if (note(); as text) {
        <p class="mt-1 max-w-2xl text-sm text-text-muted">{{ text }}</p>
      }
      <div class="mt-4"><ng-content /></div>
    </section>
  `,
})
export class UiKitSection {
  readonly title = input.required<string>();
  readonly anchor = input.required<string>();
  readonly note = input<string | null>(null);
}
