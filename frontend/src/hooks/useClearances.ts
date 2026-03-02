import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types/common';
import type {
  ClearanceRequest,
  ClearanceSummary,
  ClearanceStatus,
  ClearancePaymentStatus,
  CreateClearancePayload,
  RejectPayload,
} from '@/types/clearance';
import type { PaymentDTO } from '@/types/payment';

// ── Query keys ─────────────────────────────────────────────────────────────
export const clearanceKeys = {
  all: ['clearances'] as const,
  lists: () => [...clearanceKeys.all, 'list'] as const,
  list: (params: Record<string, unknown>) => [...clearanceKeys.lists(), params] as const,
  detail: (id: string) => [...clearanceKeys.all, 'detail', id] as const,
  summary: () => [...clearanceKeys.all, 'summary'] as const,
  // Portal-scoped
  myList: (params: Record<string, unknown>) => ['my-clearances', 'list', params] as const,
  myDetail: (id: string) => ['my-clearances', 'detail', id] as const,
};

// ─────────────────────────────────────────────────────────────────────────────
// Backoffice hooks
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Backoffice paginated clearance list with optional filters.
 */
export function useClearances(params: {
  status?: ClearanceStatus;
  paymentStatus?: ClearancePaymentStatus;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}) {
  const { page = 0, size = 20, ...filters } = params;

  return useQuery<PageResponse<ClearanceRequest>>({
    queryKey: clearanceKeys.list({ ...filters, page, size }),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/clearances', {
        params: { ...filters, page, size },
      });
      return data;
    },
  });
}

/**
 * Get a single clearance (backoffice).
 */
export function useClearance(id: string | undefined) {
  return useQuery<ClearanceRequest>({
    queryKey: clearanceKeys.detail(id!),
    queryFn: async () => {
      const { data } = await api.get(`/api/v1/clearances/${id}`);
      return data;
    },
    enabled: !!id,
  });
}

/**
 * Dashboard summary counts. Accepts optional query options (e.g. refetchInterval).
 */
export function useClearanceSummary(options?: { refetchInterval?: number }) {
  return useQuery<ClearanceSummary>({
    queryKey: clearanceKeys.summary(),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/clearances/summary');
      return data;
    },
    ...options,
  });
}

/**
 * Create a walk-in clearance request.
 */
export function useCreateWalkInClearance() {
  const qc = useQueryClient();
  return useMutation<ClearanceRequest, Error, CreateClearancePayload>({
    mutationFn: async (payload) => {
      const { data } = await api.post('/api/v1/clearances', payload);
      return data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: clearanceKeys.lists() });
      qc.invalidateQueries({ queryKey: clearanceKeys.summary() });
    },
  });
}

/**
 * Approve a clearance request.
 */
export function useApproveClearance() {
  const qc = useQueryClient();
  return useMutation<ClearanceRequest, Error, string>({
    mutationFn: async (id) => {
      const { data } = await api.post(`/api/v1/clearances/${id}/approve`);
      return data;
    },
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: clearanceKeys.detail(data.id) });
      qc.invalidateQueries({ queryKey: clearanceKeys.lists() });
      qc.invalidateQueries({ queryKey: clearanceKeys.summary() });
    },
  });
}

/**
 * Reject a clearance request.
 */
export function useRejectClearance() {
  const qc = useQueryClient();
  return useMutation<ClearanceRequest, Error, { id: string; payload: RejectPayload }>({
    mutationFn: async ({ id, payload }) => {
      const { data } = await api.post(`/api/v1/clearances/${id}/reject`, payload);
      return data;
    },
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: clearanceKeys.detail(data.id) });
      qc.invalidateQueries({ queryKey: clearanceKeys.lists() });
      qc.invalidateQueries({ queryKey: clearanceKeys.summary() });
    },
  });
}

/**
 * Release a clearance request.
 */
export function useReleaseClearance() {
  const qc = useQueryClient();
  return useMutation<ClearanceRequest, Error, string>({
    mutationFn: async (id) => {
      const { data } = await api.post(`/api/v1/clearances/${id}/release`);
      return data;
    },
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: clearanceKeys.detail(data.id) });
      qc.invalidateQueries({ queryKey: clearanceKeys.lists() });
      qc.invalidateQueries({ queryKey: clearanceKeys.summary() });
    },
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Portal hooks (resident)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * List the authenticated resident's clearance requests.
 */
export function useMyClearances(params: { page?: number; size?: number }) {
  const { page = 0, size = 20 } = params;

  return useQuery<PageResponse<ClearanceRequest>>({
    queryKey: clearanceKeys.myList({ page, size }),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/me/clearances', { params: { page, size } });
      return data;
    },
  });
}

/**
 * Get a single clearance request by ID (portal — ownership enforced server-side).
 */
export function useMyClearance(id: string | undefined) {
  return useQuery<ClearanceRequest>({
    queryKey: clearanceKeys.myDetail(id!),
    queryFn: async () => {
      const { data } = await api.get(`/api/v1/me/clearances/${id}`);
      return data;
    },
    enabled: !!id,
  });
}

/**
 * Submit a new clearance request (portal).
 */
export function useSubmitClearance() {
  const qc = useQueryClient();
  return useMutation<ClearanceRequest, Error, CreateClearancePayload>({
    mutationFn: async (payload) => {
      const { data } = await api.post('/api/v1/me/clearances', payload);
      return data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-clearances'] });
    },
  });
}

/**
 * Resubmit a rejected clearance request (portal).
 */
export function useResubmitClearance() {
  const qc = useQueryClient();
  return useMutation<ClearanceRequest, Error, { id: string; payload: CreateClearancePayload }>({
    mutationFn: async ({ id, payload }) => {
      const { data } = await api.put(`/api/v1/me/clearances/${id}`, payload);
      return data;
    },
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: clearanceKeys.myDetail(data.id) });
      qc.invalidateQueries({ queryKey: ['my-clearances'] });
    },
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Payment hooks
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resident pays for their own clearance (portal).
 * Generates a fresh UUID v4 idempotency key per invocation.
 */
export function usePayClearance() {
  const qc = useQueryClient();
  return useMutation<PaymentDTO, Error, string>({
    mutationFn: async (clearanceId) => {
      const idempotencyKey = crypto.randomUUID();
      const { data } = await api.post(`/api/v1/me/clearances/${clearanceId}/pay`, null, {
        headers: { 'Idempotency-Key': idempotencyKey },
      });
      return data;
    },
    onSuccess: (_data, clearanceId) => {
      qc.invalidateQueries({ queryKey: clearanceKeys.myDetail(clearanceId) });
      qc.invalidateQueries({ queryKey: ['my-clearances'] });
    },
  });
}

/**
 * Clerk marks a clearance as paid (cash collection).
 */
export function useMarkClearancePaid() {
  const qc = useQueryClient();
  return useMutation<PaymentDTO, Error, string>({
    mutationFn: async (clearanceId) => {
      const { data } = await api.post(`/api/v1/clearances/${clearanceId}/mark-paid`);
      return data;
    },
    onSuccess: (_data, clearanceId) => {
      qc.invalidateQueries({ queryKey: clearanceKeys.detail(clearanceId) });
      qc.invalidateQueries({ queryKey: clearanceKeys.lists() });
      qc.invalidateQueries({ queryKey: clearanceKeys.summary() });
    },
  });
}

/**
 * Get all payments associated with a clearance (backoffice).
 */
export function useClearancePayments(clearanceId: string | undefined) {
  return useQuery<PaymentDTO[]>({
    queryKey: ['clearance-payments', clearanceId],
    queryFn: async () => {
      const { data } = await api.get(`/api/v1/clearances/${clearanceId}/payments`);
      return data;
    },
    enabled: !!clearanceId,
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// PDF download helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Downloads a clearance PDF (backoffice).
 * Triggers a browser download via a temporary object URL.
 */
export async function downloadClearancePdf(clearanceId: string, clearanceNumber: string): Promise<void> {
  const response = await api.get(`/api/v1/clearances/${clearanceId}/pdf`, {
    responseType: 'blob',
  });
  triggerBlobDownload(response.data, `clearance-${clearanceNumber}.pdf`);
}

/**
 * Downloads a clearance PDF (portal — resident).
 * Triggers a browser download via a temporary object URL.
 */
export async function downloadMyClearancePdf(clearanceId: string, clearanceNumber: string): Promise<void> {
  const response = await api.get(`/api/v1/me/clearances/${clearanceId}/pdf`, {
    responseType: 'blob',
  });
  triggerBlobDownload(response.data, `clearance-${clearanceNumber}.pdf`);
}

/**
 * Creates a temporary object URL from a Blob and triggers a file download.
 */
function triggerBlobDownload(data: Blob, filename: string): void {
  const url = URL.createObjectURL(new Blob([data], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
