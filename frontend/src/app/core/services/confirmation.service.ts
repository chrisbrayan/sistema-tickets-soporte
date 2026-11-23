import { Injectable, signal } from '@angular/core';

export type ConfirmationTone = 'danger' | 'primary';

export interface ConfirmationRequest {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ConfirmationTone;
}

@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  private readonly requestState = signal<ConfirmationRequest | null>(null);
  readonly request = this.requestState.asReadonly();

  private resolver: ((confirmed: boolean) => void) | null = null;

  confirm(request: ConfirmationRequest): Promise<boolean> {
    this.resolver?.(false);
    this.requestState.set(request);
    return new Promise(resolve => {
      this.resolver = resolve;
    });
  }

  accept(): void {
    this.resolve(true);
  }

  cancel(): void {
    this.resolve(false);
  }

  private resolve(confirmed: boolean): void {
    this.requestState.set(null);
    const resolver = this.resolver;
    this.resolver = null;
    resolver?.(confirmed);
  }
}
