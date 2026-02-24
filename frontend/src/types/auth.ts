// Auth & Identity types — populated in Phase 1

export type Role = 'ADMIN' | 'CLERK' | 'APPROVER' | 'RESIDENT';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING_VERIFICATION' | 'REJECTED' | 'DEACTIVATED';
  mustChangePassword: boolean;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
  mustChangePassword?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface JwtPayload {
  sub: string;       // userId
  email: string;
  role: Role;
  mustChangePassword?: boolean;
  exp: number;
  iat: number;
}

