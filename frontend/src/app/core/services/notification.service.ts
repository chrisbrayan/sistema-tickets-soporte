import { Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../../shared/models/api-error.model';

export type NotificationType = 'success' | 'error' | 'warning' | 'info';

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  status: number | null;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private static readonly MAX_VISIBLE = 4;

  private readonly state = signal<Notification[]>([]);
  readonly notifications = this.state.asReadonly();

  private nextId = 1;
  private readonly timers = new Map<number, number>();

  success(title: string, message = '', duration = 4000): void {
    this.push({ type: 'success', title, message, status: null }, duration);
  }

  info(title: string, message = '', duration = 4500): void {
    this.push({ type: 'info', title, message, status: null }, duration);
  }

  warning(title: string, message = '', duration = 5000): void {
    this.push({ type: 'warning', title, message, status: null }, duration);
  }

  error(title: string, message = '', duration = 6500): void {
    this.push({ type: 'error', title, message, status: null }, duration);
  }

  show(error: HttpErrorResponse): void {
    const apiError = this.toApiError(error);
    this.push({
      type: 'error',
      title: this.titleFor(apiError.status),
      message: this.messageFor(apiError),
      status: apiError.status
    }, 6500);
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer !== undefined) {
      window.clearTimeout(timer);
      this.timers.delete(id);
    }
    this.state.update(notifications => notifications.filter(notification => notification.id !== id));
  }

  clear(): void {
    for (const timer of this.timers.values()) {
      window.clearTimeout(timer);
    }
    this.timers.clear();
    this.state.set([]);
  }

  private push(notification: Omit<Notification, 'id'>, duration: number): void {
    const repeated = this.state().find(current =>
      current.type === notification.type
      && current.title === notification.title
      && current.message === notification.message
      && current.status === notification.status);

    if (repeated) {
      this.scheduleDismissal(repeated.id, duration);
      return;
    }

    const toast: Notification = { id: this.nextId++, ...notification };
    const next = [...this.state(), toast];
    const removed = next.slice(0, Math.max(0, next.length - NotificationService.MAX_VISIBLE));
    for (const item of removed) {
      const timer = this.timers.get(item.id);
      if (timer !== undefined) {
        window.clearTimeout(timer);
        this.timers.delete(item.id);
      }
    }
    this.state.set(next.slice(-NotificationService.MAX_VISIBLE));
    this.scheduleDismissal(toast.id, duration);
  }

  private scheduleDismissal(id: number, duration: number): void {
    const currentTimer = this.timers.get(id);
    if (currentTimer !== undefined) {
      window.clearTimeout(currentTimer);
    }
    this.timers.set(id, window.setTimeout(() => this.dismiss(id), duration));
  }

  private titleFor(status: number): string {
    switch (status) {
      case 400: return 'Datos inválidos';
      case 401: return 'Sesión requerida';
      case 403: return 'Acceso denegado';
      case 404: return 'Recurso no encontrado';
      case 409: return 'Conflicto';
      default: return 'No se pudo completar la operación';
    }
  }

  private messageFor(error: ApiError): string {
    if (error.status === 400) return error.message || 'Revisa los campos e inténtalo nuevamente.';
    if (error.status === 401) return 'Tu sesión no es válida o ha expirado. Inicia sesión nuevamente.';
    if (error.status === 403) return 'Tu rol no tiene permisos para realizar esta acción.';
    if (error.status === 404) return error.message || 'El recurso solicitado no existe.';
    if (error.status === 409) return error.message || 'La operación entra en conflicto con información existente.';
    if (error.status === 0) return 'No se pudo conectar con el servidor. Verifica que el backend esté ejecutándose.';
    return error.message || 'Inténtalo nuevamente.';
  }

  private toApiError(error: HttpErrorResponse): ApiError {
    if (error.error && typeof error.error === 'object' && 'message' in error.error) {
      return error.error as ApiError;
    }
    return { status: error.status, message: error.statusText || '' };
  }
}
