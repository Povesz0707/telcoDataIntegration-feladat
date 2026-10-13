import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';
import { AuthResponse, CurrentUser, LoginRequest, RefreshTokenRequest } from '../models/auth.models';

const ACCESS_TOKEN_KEY = 'telco.accessToken';
const REFRESH_TOKEN_KEY = 'telco.refreshToken';
const USER_KEY = 'telco.currentUser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/auth';
  private readonly authenticated = signal(this.hasValidAccessToken());
  private readonly currentUser = signal<CurrentUser | null>(this.readUser());

  isAuthenticated(): boolean {
    return this.authenticated();
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  username(): string | null {
    return this.currentUser()?.username ?? null;
  }

  accessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY);
  }

  hasRefreshToken(): boolean {
    return !!sessionStorage.getItem(REFRESH_TOKEN_KEY);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
      tap(response => this.storeSession(response))
    );
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }
    const request: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, request).pipe(
      tap(response => this.storeSession(response))
    );
  }

  restoreSession(): Observable<boolean> {
    if (this.hasValidAccessToken()) {
      this.authenticated.set(true);
      return of(true);
    }
    if (!this.hasRefreshToken()) {
      this.clearSession();
      return of(false);
    }
    return this.refresh().pipe(
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      })
    );
  }

  logout(): Observable<void> {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    this.clearSession();
    if (!refreshToken) {
      return of(void 0);
    }
    return this.http.post<void>(`${this.baseUrl}/logout`, { refreshToken }).pipe(
      catchError(() => of(void 0))
    );
  }

  clearSession(): void {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    this.authenticated.set(false);
    this.currentUser.set(null);
  }

  private storeSession(response: AuthResponse): void {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    sessionStorage.setItem(USER_KEY, JSON.stringify({ username: response.username, role: response.role }));
    this.authenticated.set(true);
    this.currentUser.set({ username: response.username, role: response.role });
  }

  private hasValidAccessToken(): boolean {
    const token = this.accessToken();
    if (!token) {
      return false;
    }
    try {
      const payload = JSON.parse(atob(token.split('.')[1])) as { exp?: number };
      return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private readUser(): CurrentUser | null {
    const stored = sessionStorage.getItem(USER_KEY);
    if (!stored) {
      return null;
    }
    try {
      return JSON.parse(stored) as CurrentUser;
    } catch {
      return null;
    }
  }
}
