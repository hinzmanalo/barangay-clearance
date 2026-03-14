// Resident types

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type ResidentStatus = 'ACTIVE' | 'INACTIVE';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING_VERIFICATION' | 'REJECTED' | 'DEACTIVATED';

export interface Resident {
  id: string;
  userId?: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  birthDate: string;
  gender: Gender;
  address: string;
  contactNumber?: string;
  email?: string;
  status: ResidentStatus;
  hasPortalAccount: boolean;
  portalStatus?: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateResidentPayload {
  firstName: string;
  middleName?: string;
  lastName: string;
  birthDate: string;
  gender: Gender;
  address: string;
  contactNumber?: string;
  email?: string;
}

export interface UpdateResidentPayload {
  firstName?: string;
  middleName?: string;
  lastName?: string;
  birthDate?: string;
  gender?: Gender;
  address?: string;
  contactNumber?: string;
  email?: string;
  status?: ResidentStatus;
  userId?: string;
}
