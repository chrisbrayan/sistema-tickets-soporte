import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../../shared/models/api-error.model';

export function toApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    if (error.error && typeof error.error === 'object' && 'message' in error.error) {
      return error.error as ApiError;
    }

    return {
      status: error.status,
      message: error.statusText || 'No se pudo completar la solicitud.'
    };
  }

  if (error instanceof Error && error.message) {
    return { status: 0, message: error.message };
  }

  return {
    status: 0,
    message: 'No se pudo completar la solicitud.'
  };
}
