import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types/common';
import type {
  ReportRow,
  ClearanceStatus,
  ClearancePaymentStatus,
  Purpose,
} from '@/types/clearance';

// ── Query keys ─────────────────────────────────────────────────────────────

export const reportKeys = {
  all: ['reports'] as const,
  clearances: (params: Record<string, unknown>) =>
    [...reportKeys.all, 'clearances', params] as const,
};

// ── Hook ───────────────────────────────────────────────────────────────────

export interface ReportFilters {
  status?: ClearanceStatus;
  paymentStatus?: ClearancePaymentStatus;
  purpose?: Purpose;
  purok?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

/**
 * Fetches the paginated clearance issuance report from
 * {@code GET /api/v1/reports/clearances}.
 */
export function useReports(filters: ReportFilters) {
  const { page = 0, size = 20, ...rest } = filters;

  // Remove empty-string values so they are omitted from the query string.
  const cleanFilters = Object.fromEntries(
    Object.entries(rest).filter(([, v]) => v !== '' && v !== undefined)
  );

  return useQuery<PageResponse<ReportRow>>({
    queryKey: reportKeys.clearances({ ...cleanFilters, page, size }),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/reports/clearances', {
        params: { ...cleanFilters, page, size },
      });
      return data;
    },
  });
}
