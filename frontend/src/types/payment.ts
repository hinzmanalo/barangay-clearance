// Payment types — Phase 4

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type PaymentMethod = 'STUB' | 'CASH';

/**
 * Response DTO from POST /clearances/{id}/payments or POST /me/clearances/{id}/pay.
 * `idempotent` is true when the response is a cached replay of a previous call.
 */
export interface PaymentDTO {
  id: string;
  clearanceRequestId: string;
  amount: number;
  idempotencyKey: string;
  initiatedByUserId: string;
  paymentMethod: PaymentMethod;
  provider: string;
  status: PaymentStatus;
  idempotent: boolean;
  createdAt: string;
  updatedAt: string;
}

/** @deprecated Use PaymentDTO instead */
export type Payment = PaymentDTO;
