// Clearance types — populated in Phase 3

export type ClearanceStatus = 'DRAFT' | 'FOR_APPROVAL' | 'APPROVED' | 'REJECTED' | 'RELEASED';
export type PaymentStatus = 'UNPAID' | 'PAID';
export type Urgency = 'STANDARD' | 'RUSH';

export interface ClearanceRequest {
  id: string;
  clearanceNumber?: string;
  residentId: string;
  requestedBy: string;
  purpose: string;
  urgency: Urgency;
  feeAmount: number;
  status: ClearanceStatus;
  paymentStatus: PaymentStatus;
  notes?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  issuedAt?: string;
  createdAt: string;
  updatedAt: string;
}
