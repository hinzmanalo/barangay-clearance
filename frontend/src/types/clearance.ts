// Clearance types — Phase 3

export type ClearanceStatus = 'DRAFT' | 'FOR_APPROVAL' | 'APPROVED' | 'REJECTED' | 'RELEASED';
export type ClearancePaymentStatus = 'UNPAID' | 'PAID' | 'WAIVED';
export type Urgency = 'STANDARD' | 'RUSH';
export type Purpose =
  | 'EMPLOYMENT'
  | 'TRAVEL_ABROAD'
  | 'SCHOLARSHIP'
  | 'LOAN'
  | 'BUSINESS_PERMIT'
  | 'LEGAL'
  | 'CEDULA'
  | 'OTHER';

export const PURPOSE_LABELS: Record<Purpose, string> = {
  EMPLOYMENT: 'Employment',
  TRAVEL_ABROAD: 'Travel Abroad',
  SCHOLARSHIP: 'Scholarship',
  LOAN: 'Loan',
  BUSINESS_PERMIT: 'Business Permit',
  LEGAL: 'Legal',
  CEDULA: 'Cedula',
  OTHER: 'Other',
};

export const STATUS_LABELS: Record<ClearanceStatus, string> = {
  DRAFT: 'Draft',
  FOR_APPROVAL: 'For Approval',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  RELEASED: 'Released',
};

export const PAYMENT_STATUS_LABELS: Record<ClearancePaymentStatus, string> = {
  UNPAID: 'Unpaid',
  PAID: 'Paid',
  WAIVED: 'Waived',
};

export interface ClearanceRequest {
  id: string;
  clearanceNumber?: string;
  residentId: string;
  residentName?: string;
  requestedBy: string;
  purpose: Purpose;
  purposeOther?: string;
  urgency: Urgency;
  feeAmount: number;
  copies: number;
  status: ClearanceStatus;
  paymentStatus: ClearancePaymentStatus;
  notes?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  issuedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClearancePayload {
  purpose: Purpose;
  purposeOther?: string;
  urgency: Urgency;
  copies: number;
  notes?: string;
  residentId?: string; // walk-in only
}

export interface RejectPayload {
  reason: string;
}

export interface ClearanceSummary {
  pendingApproval: number;
  approved: number;
  awaitingPayment: number;
  releasedToday: number;
}

// ── Reports ──────────────────────────────────────────────────────────────────

export interface ReportRow {
  clearanceNumber?: string;
  residentFullName: string;
  purpose: Purpose;
  urgency: Urgency;
  status: ClearanceStatus;
  paymentStatus: ClearancePaymentStatus;
  issuedAt?: string;
  createdAt: string;
}
