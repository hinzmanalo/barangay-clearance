// Payment types — populated in Phase 4

export interface Payment {
  id: string;
  clearanceRequestId: string;
  amount: number;
  idempotencyKey: string;
  initiatedByUserId: string;
  provider: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  createdAt: string;
  updatedAt: string;
}
