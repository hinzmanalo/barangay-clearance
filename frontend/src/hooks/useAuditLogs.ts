import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types/common';
import type { AuditLog, AuditLogFilters } from '@/types/audit';

// ── Query keys ────────────────────────────────────────────────────────────

export const auditKeys = {
  all: ['audit-logs'] as const,
  lists: () => [...auditKeys.all, 'list'] as const,
  list: (params: Record<string, unknown>) => [...auditKeys.lists(), params] as const,
  entity: (type: string, id: string) => [...auditKeys.all, 'entity', type, id] as const,
};

// ── Types ─────────────────────────────────────────────────────────────────

export interface AuditLogQueryParams extends AuditLogFilters {
  page?: number;
  size?: number;
  sort?: string;
}

// ── Hooks ─────────────────────────────────────────────────────────────────

/**
 * Paginated, filtered list of audit log entries.
 * Accessible by ADMIN role only.
 */
export function useAuditLogs(params: AuditLogQueryParams = {}) {
  const { page = 0, size = 20, sort = 'createdAt,desc', ...filters } = params;

  return useQuery<PageResponse<AuditLog>>({
    queryKey: auditKeys.list({ page, size, sort, ...filters }),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/audit-logs', {
        params: {
          page,
          size,
          sort,
          action: filters.action || undefined,
          entityType: filters.entityType || undefined,
          userId: filters.userId || undefined,
          from: filters.from || undefined,
          to: filters.to || undefined,
        },
      });
      return data;
    },
  });
}

/**
 * Audit history for a specific entity.
 */
export function useEntityAuditLogs(
  entityType: string,
  entityId: string | undefined,
  params: { page?: number; size?: number } = {}
) {
  const { page = 0, size = 20 } = params;

  return useQuery<PageResponse<AuditLog>>({
    queryKey: auditKeys.entity(entityType, entityId!),
    queryFn: async () => {
      const { data } = await api.get(
        `/api/v1/audit-logs/entity/${entityType}/${entityId}`,
        { params: { page, size, sort: 'createdAt,desc' } }
      );
      return data;
    },
    enabled: !!entityId,
  });
}
