import { Component, Input } from '@angular/core';
import { initials } from './ticket-ui';

@Component({
  selector: 'app-avatar',
  template: `<span [class]="classes">{{ initials(name) }}</span>`
})
export class AvatarComponent {
  @Input() name: string | null | undefined = '';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';

  get classes(): string {
    const sizes: Record<'sm' | 'md' | 'lg', string> = {
      sm: 'h-8 w-8 text-xs',
      md: 'h-10 w-10 text-sm',
      lg: 'h-12 w-12 text-base'
    };
    return `inline-flex shrink-0 items-center justify-center rounded-full bg-sky-100 font-bold text-navy-800 ${sizes[this.size]}`;
  }

  initials(value: string | null | undefined): string {
    return initials(value);
  }
}
