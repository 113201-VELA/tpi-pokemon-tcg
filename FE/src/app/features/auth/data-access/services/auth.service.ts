import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { AuthResponse, AuthUser, LoginRequest, RegisterRequest } from '../../domain/models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private readonly _currentUser = signal<AuthUser | null>(this.loadUserFromStorage());

  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  login(request: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => this.handleAuthResponse(response))
    );
  }

  logout(): void {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    this._currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  private handleAuthResponse(response: AuthResponse): void {
    const user: AuthUser = {
      id: response.id,
      username: response.username,
      email: response.email
    };
    localStorage.setItem('auth_token', response.token);
    localStorage.setItem('auth_user', JSON.stringify(user));
    this._currentUser.set(user);
  }

  private loadUserFromStorage(): AuthUser | null {
    const raw = localStorage.getItem('auth_user');
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }
}
