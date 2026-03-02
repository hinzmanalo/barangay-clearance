import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types/common';
import type {
  StaffUser,
  CreateStaffPayload,
  UpdateStaffPayload,
  UpdateRolePayload,
  AdminResetPasswordPayload,
  UpdateProfilePayload,
} from '@/types/auth';

// ── Query keys ────────────────────────────────────────────────────────────
export const userKeys = {
  all: ['users'] as const,
  lists: () => [...userKeys.all, 'list'] as const,
  list: (params: Record<string, unknown>) => [...userKeys.lists(), params] as const,
  detail: (id: string) => [...userKeys.all, 'detail', id] as const,
  me: () => [...userKeys.all, 'me'] as const,
};

// ── Hooks ─────────────────────────────────────────────────────────────────

export interface UseUsersParams {
  role?: string;
  status?: string;
  search?: string;
  page?: number;
  size?: number;
}

/**
 * Paginated + filtered list of staff users.
 */
export function useUsers(params: UseUsersParams = {}) {
  const { role, status, search, page = 0, size = 20 } = params;

  return useQuery<PageResponse<StaffUser>>({
    queryKey: userKeys.list({ role, status, search, page, size }),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/admin/users', {
        params: {
          role: role || undefined,
          status: status || undefined,
          search: search || undefined,
          page,
          size,
        },
      });
      return data;
    },
  });
}

/**
 * Get a single staff user by ID.
 */
export function useUser(id: string | undefined) {
  return useQuery<StaffUser>({
    queryKey: userKeys.detail(id!),
    queryFn: async () => {
      const { data } = await api.get(`/api/v1/admin/users/${id}`);
      return data;
    },
    enabled: !!id,
  });
}

/**
 * Create a new staff account.
 */
export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation<StaffUser, Error, CreateStaffPayload>({
    mutationFn: async (payload) => {
      const { data } = await api.post('/api/v1/admin/users', payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

/**
 * Update a staff user's profile (name and/or email).
 */
export function useUpdateUser(id: string) {
  const queryClient = useQueryClient();
  return useMutation<StaffUser, Error, UpdateStaffPayload>({
    mutationFn: async (payload) => {
      const { data } = await api.put(`/api/v1/admin/users/${id}`, payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

/**
 * Change a staff user's role.
 */
export function useUpdateUserRole(id: string) {
  const queryClient = useQueryClient();
  return useMutation<StaffUser, Error, UpdateRolePayload>({
    mutationFn: async (payload) => {
      const { data } = await api.put(`/api/v1/admin/users/${id}/role`, payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

/**
 * Reactivate a deactivated user.
 */
export function useActivateUser(id: string) {
  const queryClient = useQueryClient();
  return useMutation<StaffUser, Error, void>({
    mutationFn: async () => {
      const { data } = await api.put(`/api/v1/admin/users/${id}/activate`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

/**
 * Deactivate a user account.
 */
export function useDeactivateUser(id: string) {
  const queryClient = useQueryClient();
  return useMutation<StaffUser, Error, void>({
    mutationFn: async () => {
      const { data } = await api.put(`/api/v1/admin/users/${id}/deactivate`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: userKeys.lists() });
    },
  });
}

/**
 * Force-reset a user's password (admin-initiated).
 */
export function useAdminResetPassword(id: string) {
  return useMutation<void, Error, AdminResetPasswordPayload>({
    mutationFn: async (payload) => {
      await api.post(`/api/v1/admin/users/${id}/reset-password`, payload);
    },
  });
}

/**
 * Get the currently authenticated user's own profile.
 */
export function useCurrentUser() {
  return useQuery<StaffUser>({
    queryKey: userKeys.me(),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/users/me');
      return data;
    },
  });
}

/**
 * Update the currently authenticated user's own profile.
 */
export function useUpdateCurrentUser() {
  const queryClient = useQueryClient();
  return useMutation<StaffUser, Error, UpdateProfilePayload>({
    mutationFn: async (payload) => {
      const { data } = await api.put('/api/v1/users/me', payload);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: userKeys.me() });
    },
  });
}
