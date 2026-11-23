import { Component, HostListener, inject } from '@angular/core';
import { ConfirmationService } from '../../core/services/confirmation.service';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-confirm-dialog',
  imports: [IconComponent],
  styles: [`
    @keyframes confirm-in {
      from { opacity: 0; transform: translateY(10px) scale(0.98); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    .confirm-enter { animation: confirm-in 0.16s ease-out; }
  `],
  template: `
    @if (confirmation.request(); as request) {
      <div class="fixed inset-0 z-[55] flex items-center justify-center bg-navy-950/60 p-4 backdrop-blur-sm"
           (click)="confirmation.cancel()">
        <div class="confirm-enter card w-full max-w-md p-6 shadow-2xl"
             role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title" aria-describedby="confirm-dialog-message"
             (click)="$event.stopPropagation()">
          <div class="flex items-start gap-4">
            <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full"
                  [class]="request.tone === 'primary' ? 'bg-sky-100 text-sky-700' : 'bg-rose-100 text-rose-700'">
              <app-icon [name]="request.tone === 'primary' ? 'check' : 'trash'" class="h-5 w-5" />
            </span>
            <div class="min-w-0 flex-1">
              <h2 id="confirm-dialog-title" class="text-lg font-semibold text-navy-900">{{ request.title }}</h2>
              <p id="confirm-dialog-message" class="mt-2 text-sm leading-6 text-slate-600">{{ request.message }}</p>
            </div>
            <button type="button" (click)="confirmation.cancel()" aria-label="Cerrar"
                    class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100">
              <app-icon name="close" class="h-5 w-5" />
            </button>
          </div>

          <div class="mt-6 flex justify-end gap-3">
            <button type="button" (click)="confirmation.cancel()" class="btn-ghost">
              {{ request.cancelLabel || 'Cancelar' }}
            </button>
            <button type="button" (click)="confirmation.accept()"
                    [class]="request.tone === 'primary' ? 'btn-primary' : 'btn-danger'">
              {{ request.confirmLabel || 'Aceptar' }}
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ConfirmDialogComponent {
  readonly confirmation = inject(ConfirmationService);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.confirmation.request()) {
      this.confirmation.cancel();
    }
  }
}
