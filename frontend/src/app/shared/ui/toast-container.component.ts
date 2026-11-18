import { Component, inject } from '@angular/core';
import { NotificationService, NotificationType } from '../../core/services/notification.service';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-toast-container',
  imports: [IconComponent],
  styles: [`
    @keyframes toast-in {
      from { opacity: 0; transform: translateY(12px) scale(0.98); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    .toast-enter { animation: toast-in 0.18s ease-out; }
  `],
  template: `
    <div class="pointer-events-none fixed inset-x-0 bottom-0 z-[60] flex flex-col items-end gap-3 p-4 sm:inset-x-auto sm:bottom-6 sm:right-6 sm:p-0"
         aria-live="polite" aria-atomic="false">
      @for (notification of notifications.notifications(); track notification.id) {
        <aside class="toast-enter pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-xl border bg-white p-4 shadow-xl"
               [class]="borderClass(notification.type)"
               [attr.role]="notification.type === 'error' ? 'alert' : 'status'">
          <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-bold"
                [class]="badgeClass(notification.type)">
            @if (notification.status !== null) {
              {{ notification.status }}
            } @else if (notification.type === 'success') {
              <app-icon name="check" class="h-4 w-4" />
            } @else {
              {{ symbolFor(notification.type) }}
            }
          </span>
          <div class="min-w-0 flex-1">
            <p class="font-semibold text-slate-900">{{ notification.title }}</p>
            @if (notification.message && notification.message.toLowerCase() !== notification.title.toLowerCase()) {
              <p class="mt-1 text-sm leading-5 text-slate-600">{{ notification.message }}</p>
            }
          </div>
          <button type="button" (click)="notifications.dismiss(notification.id)" aria-label="Cerrar notificación"
                  class="text-xl leading-none text-slate-400 transition hover:text-slate-700">×</button>
        </aside>
      }
    </div>
  `
})
export class ToastContainerComponent {
  readonly notifications = inject(NotificationService);

  borderClass(type: NotificationType): string {
    switch (type) {
      case 'success': return 'border-emerald-200';
      case 'error': return 'border-rose-200';
      case 'warning': return 'border-amber-200';
      default: return 'border-sky-200';
    }
  }

  badgeClass(type: NotificationType): string {
    switch (type) {
      case 'success': return 'bg-emerald-100 text-emerald-700';
      case 'error': return 'bg-rose-100 text-rose-700';
      case 'warning': return 'bg-amber-100 text-amber-700';
      default: return 'bg-sky-100 text-sky-700';
    }
  }

  symbolFor(type: NotificationType): string {
    switch (type) {
      case 'error': return '!';
      case 'warning': return '!';
      default: return 'i';
    }
  }
}
