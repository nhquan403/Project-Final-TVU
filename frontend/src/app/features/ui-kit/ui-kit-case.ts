import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Một ô trạng thái: nhãn trạng thái ở trên, ví dụ thật ở dưới. */
@Component({
  selector: 'ui-kit-case',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rounded-md border border-border bg-surface p-3">
      <p class="mb-2 text-xs font-semibold uppercase tracking-wide text-text-muted">
        {{ state() }}
      </p>
      <ng-content />
    </div>
  `,
})
export class UiKitCase {
  readonly state = input.required<string>();
}
