import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-icon',
  host: { class: 'inline-flex shrink-0' },
  template: `
    <svg class="h-full w-full" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      @switch (name) {
        @case ('dashboard') {
          <path d="M3 3h7v9H3zM14 3h7v5h-7zM14 12h7v9h-7zM3 16h7v5H3z" />
        }
        @case ('ticket') {
          <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
          <path d="M13 5v2M13 11v2M13 17v2" />
        }
        @case ('plus') { <path d="M12 5v14M5 12h14" /> }
        @case ('users') {
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" />
        }
        @case ('search') { <path d="M21 21l-4.3-4.3M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16" /> }
        @case ('logout') {
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <path d="M16 17l5-5-5-5M21 12H9" />
        }
        @case ('menu') { <path d="M3 6h18M3 12h18M3 18h18" /> }
        @case ('close') { <path d="M18 6 6 18M6 6l12 12" /> }
        @case ('back') { <path d="M19 12H5M12 19l-7-7 7-7" /> }
        @case ('check') { <path d="M20 6 9 17l-5-5" /> }
        @case ('edit') { <path d="M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" /> }
        @case ('clock') { <path d="M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20M12 6v6l4 2" /> }
        @case ('trash') {
          <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          <path d="M10 11v6M14 11v6" />
        }
        @case ('category') { <path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 12 9 5 9-5M3 16l9 5 9-5" /> }
      }
    </svg>
  `
})
export class IconComponent {
  @Input() name = '';
}
