import { Routes } from '@angular/router';
import { uiKitRoutes } from './features/ui-kit/ui-kit.routes';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/health/health').then((m) => m.HealthPage),
    title: 'Homestay TVH',
  },
  // Rỗng ở bản production nhờ fileReplacements — xem ui-kit.routes.ts.
  ...uiKitRoutes,
];
