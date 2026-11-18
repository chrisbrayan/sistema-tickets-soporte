import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse } from '../../shared/models/auth.model';
import { Usuario } from '../../shared/models/usuario.model';

const TOKEN_KEY = 'sistema-tickets-token';
const LOGIN_KEY = 'sistema-tickets-login';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly loginState = signal<LoginResponse | null>(this.readLogin());
  private readonly tokenState = signal<string | null>(this.loginState()?.token ?? null);

  readonly currentLogin = this.loginState.asReadonly();

  constructor(private readonly http: HttpClient) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap(response => {
        sessionStorage.setItem(TOKEN_KEY, response.token);
        sessionStorage.setItem(LOGIN_KEY, JSON.stringify(response));
        this.tokenState.set(response.token);
        this.loginState.set(response);
      })
    );
  }

  registrar(request: { nombre: string; email: string; password: string }): Observable<Usuario> {
    return this.http.post<Usuario>(`${environment.apiUrl}/auth/registro`, request);
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(LOGIN_KEY);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(LOGIN_KEY);
    this.tokenState.set(null);
    this.loginState.set(null);
  }

  token(): string | null {
    return this.tokenState();
  }

  isAuthenticated(): boolean {
    return Boolean(this.tokenState() && !this.isExpired(this.loginState()?.expiresAt));
  }

  role(): LoginResponse['role'] | null {
    return this.loginState()?.role ?? null;
  }

  subject(): string | null {
    return this.loginState()?.subject ?? null;
  }

  userId(): number | null {
    return this.loginState()?.userId ?? null;
  }

  private readLogin(): LoginResponse | null {
    const stored = sessionStorage.getItem(LOGIN_KEY);
    if (!stored) {
      return null;
    }

    try {
      const login = JSON.parse(stored) as LoginResponse;
      return this.isExpired(login.expiresAt) ? null : login;
    } catch {
      sessionStorage.removeItem(LOGIN_KEY);
      return null;
    }
  }

  private isExpired(expiresAt: string | undefined): boolean {
    return !expiresAt || Number.isNaN(Date.parse(expiresAt)) || Date.parse(expiresAt) <= Date.now();
  }
}
