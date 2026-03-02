// Auth & Identity types — populated in Phase 1

export type Role = 'ADMIN' | 'CLERK' | 'APPROVER' | 'RESIDENT';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING_VERIFICATION' | 'REJECTED' | 'DEACTIVATED';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  status: UserStatus;
  mustChangePassword: boolean;
}

/** Full staff user record returned from /admin/users */
export interface StaffUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  status: UserStatus;
  mustChangePassword: boolean;
  createdAt: string;
}

export interface CreateStaffPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: 'ADMIN' | 'CLERK' | 'APPROVER';
}

export interface UpdateStaffPayload {
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface UpdateRolePayload {
  role: 'ADMIN' | 'CLERK' | 'APPROVER';
}

export interface AdminResetPasswordPayload {
  newPassword: string;
}

export interface UpdateProfilePayload {
  firstName?: string;
  lastName?: string;
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


