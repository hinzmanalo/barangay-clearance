// Auth & Identity types — populated in Phase 1

export type Role = 'ADMIN' | 'CLERK' | 'APPROVER' | 'RESIDENT';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  status: 'ACTIVE' | 'INACTIVE';
  mustChangePassword: boolean;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}
