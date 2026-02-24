// Resident types — populated in Phase 2

export interface Resident {
  id: string;
  userId?: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  birthDate: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  address: string;
  contactNumber?: string;
  email?: string;
  status: 'ACTIVE' | 'INACTIVE';
  hasPortalAccount: boolean;
  createdAt: string;
  updatedAt: string;
}
