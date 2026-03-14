import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types/common';
import type { Resident, CreateResidentPayload, UpdateResidentPayload } from '@/types/resident';

// ── Query keys ────────────────────────────────────────────────────────────
export const residentKeys = {
  all: ['residents'] as const,
  lists: () => [...residentKeys.all, 'list'] as const,
  list: (params: Record<string, unknown>) => [...residentKeys.lists(), params] as const,
  detail: (id: string) => [...residentKeys.all, 'detail', id] as const,
  pending: () => [...residentKeys.all, 'pending'] as const,
};

// ── Hooks ─────────────────────────────────────────────────────────────────

/**
 * Paginated resident search.
 */
export function useResidents(params: {
  q?: string;
  purok?: string;
  page?: number;
  size?: number;
}) {
  const { q = '', purok = '', page = 0, size = 20 } = params;

  return useQuery<PageResponse<Resident>>({
    queryKey: residentKeys.list({ q, purok, page, size }),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/residents', {
        params: { q: q || undefined, purok: purok || undefined, page, size },
      });
      return data;
    },
  });
}

/**
 * Get a single resident by ID.
 */
export function useResident(id: string | undefined) {
  return useQuery<Resident>({
    queryKey: residentKeys.detail(id!),
    queryFn: async () => {
      const { data } = await api.get(`/api/v1/residents/${id}`);
      return data;
    },
    enabled: !!id,
  });
}

/**
 * List residents with pending portal accounts.
 */
export function usePendingResidents() {
  return useQuery<Resident[]>({
    queryKey: residentKeys.pending(),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/residents/pending-users');
      return data;
    },
  });
}

/**
 * Create a new walk-in resident.
 */
export function useCreateResident() {
  const queryClient = useQueryClient();
  return useMutation<Resident, Error, CreateResidentPayload>({
    mutationFn: async (payload) => {
      const { data } = await api.post('/api/v1/residents', payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: residentKeys.lists() });
    },
  });
}

/**
 * Update an existing resident.
 */
export function useUpdateResident(id: string) {
  const queryClient = useQueryClient();
  return useMutation<Resident, Error, UpdateResidentPayload>({
    mutationFn: async (payload) => {
      const { data } = await api.put(`/api/v1/residents/${id}`, payload);
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(residentKeys.detail(id), updated);
      queryClient.invalidateQueries({ queryKey: residentKeys.lists() });
    },
  });
}

/**
 * Activate a pending portal account.
 */
export function useActivateResident() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: async (userId) => {
      await api.post(`/api/v1/residents/users/${userId}/activate`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: residentKeys.all });
    },
  });
}

/**
 * Reject a pending portal account.
 */
export function useRejectResident() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: async (userId) => {
      await api.post(`/api/v1/residents/users/${userId}/reject`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: residentKeys.all });
    },
  });
}
