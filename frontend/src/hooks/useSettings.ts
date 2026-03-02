import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type {
  BarangaySettings,
  FeeConfig,
  UpdateBarangaySettingsPayload,
  UpdateFeeConfigPayload,
} from '@/types/settings';

// ── Query keys ────────────────────────────────────────────────────────────
export const settingsKeys = {
  all: ['settings'] as const,
  barangay: () => [...settingsKeys.all, 'barangay'] as const,
  fees: () => [...settingsKeys.all, 'fees'] as const,
};

// ── Hooks ─────────────────────────────────────────────────────────────────

/**
 * Fetches barangay settings. Admin only.
 */
export function useBarangaySettings() {
  return useQuery<BarangaySettings>({
    queryKey: settingsKeys.barangay(),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/settings');
      return data;
    },
  });
}

/**
 * Updates the barangay profile fields (name, municipality, province, captain).
 */
export function useUpdateBarangaySettings() {
  const queryClient = useQueryClient();

  return useMutation<BarangaySettings, Error, UpdateBarangaySettingsPayload>({
    mutationFn: async (payload) => {
      const { data } = await api.put('/api/v1/settings', payload);
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(settingsKeys.barangay(), updated);
    },
  });
}

/**
 * Uploads the barangay logo (multipart).
 */
export function useUploadLogo() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, File>({
    mutationFn: async (file) => {
      const formData = new FormData();
      formData.append('file', file);
      await api.post('/api/v1/settings/logo', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
    },
    onSuccess: () => {
      // Invalidate barangay settings so hasLogo / logoMimeType refresh
      queryClient.invalidateQueries({ queryKey: settingsKeys.barangay() });
    },
  });
}

/**
 * Fetches fee configuration. Admin only.
 */
export function useFeeConfig() {
  return useQuery<FeeConfig>({
    queryKey: settingsKeys.fees(),
    queryFn: async () => {
      const { data } = await api.get('/api/v1/settings/fees');
      return data;
    },
  });
}

/**
 * Updates the standard and rush fees.
 */
export function useUpdateFeeConfig() {
  const queryClient = useQueryClient();

  return useMutation<FeeConfig, Error, UpdateFeeConfigPayload>({
    mutationFn: async (payload) => {
      const { data } = await api.put('/api/v1/settings/fees', payload);
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(settingsKeys.fees(), updated);
    },
  });
}
