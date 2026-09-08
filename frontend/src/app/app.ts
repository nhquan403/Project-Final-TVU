import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Khung ứng dụng. Nội dung do router quyết định. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {}
