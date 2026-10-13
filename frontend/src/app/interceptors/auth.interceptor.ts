import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, switchMap, throwError } from 'rxjs';
import { AuthResponse } from '../models/auth.models';
import { AuthService } from '../services/auth.service';

let refreshRequest$: Observable<AuthResponse> | null = null;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requestWithToken = addAccessToken(request, auth.accessToken());

  return next(requestWithToken).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isAuthenticationRequest(request.url) || !auth.hasRefreshToken()) {
        if (error.status === 401 && !auth.hasRefreshToken()) {
          auth.clearSession();
          navigateToLogin(router);
        }
        return throwError(() => error);
      }

      refreshRequest$ ??= auth.refresh().pipe(
        finalize(() => refreshRequest$ = null)
      );
      return refreshRequest$.pipe(
        switchMap(response => next(addAccessToken(request, response.accessToken))),
        catchError(refreshError => {
          auth.clearSession();
          navigateToLogin(router);
          return throwError(() => refreshError);
        })
      );
    })
  );
};

function addAccessToken(request: Parameters<HttpInterceptorFn>[0], token: string | null) {
  return token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
}

function isAuthenticationRequest(url: string): boolean {
  return url.includes('/api/auth/login') || url.includes('/api/auth/refresh') || url.includes('/api/auth/logout');
}

function navigateToLogin(router: Router): void {
  if (router.url !== '/login') {
    void router.navigateByUrl('/login');
  }
}
