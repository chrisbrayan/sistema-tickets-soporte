import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { catchError, throwError } from "rxjs";
import { NotificationService } from "../services/notification.service";

const AUTH_ENDPOINTS = ["/auth/login", "/auth/registro"];

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationService);

  return next(request).pipe(
    catchError((error) => {
      if (error instanceof HttpErrorResponse) {
        const isAuthRequest = AUTH_ENDPOINTS.some((endpoint) =>
          request.url.includes(endpoint),
        );
        if (!isAuthRequest) {
          notifications.show(error);
        }
      }
      return throwError(() => error);
    }),
  );
};
