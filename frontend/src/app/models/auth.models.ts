export type UserRole = 'USER' | 'ADMIN';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresInSeconds: number;
  username: string;
  role: UserRole;
}

export interface CurrentUser {
  username: string;
  role: UserRole;
}
