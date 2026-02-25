'use client';

import { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { useFeeConfig, useUpdateFeeConfig } from '@/hooks/useSettings';
import { AxiosError } from 'axios';

// ── Zod schema ───────────────────────────────────────────────────────────

const feesSchema = z.object({
  standardFee: z
    .number({ required_error: 'Standard fee is required', invalid_type_error: 'Must be a number' })
    .min(0, 'Standard fee must be non-negative')
    .multipleOf(0.01, 'Maximum 2 decimal places'),
  rushFee: z
    .number({ required_error: 'Rush fee is required', invalid_type_error: 'Must be a number' })
    .min(0, 'Rush fee must be non-negative')
    .multipleOf(0.01, 'Maximum 2 decimal places'),
});

type FeesForm = z.infer<typeof feesSchema>;

// ── Component ────────────────────────────────────────────────────────────

export default function FeesPage() {
  const { role, isLoading: authLoading } = useAuth();
  const router = useRouter();

  // Redirect non-admins
  useEffect(() => {
    if (!authLoading && role !== 'ADMIN') {
      router.replace('/backoffice/dashboard');
    }
  }, [authLoading, role, router]);

  const { data: feeConfig, isLoading } = useFeeConfig();
  const updateMutation = useUpdateFeeConfig();

  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FeesForm>({
    resolver: zodResolver(feesSchema),
  });

  // Populate form when fee config loads
  useEffect(() => {
    if (feeConfig) {
      reset({
        standardFee: feeConfig.standardFee,
        rushFee: feeConfig.rushFee,
      });
    }
  }, [feeConfig, reset]);

  const showToast = useCallback((message: string, type: 'success' | 'error') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  }, []);

  const onSubmit = async (data: FeesForm) => {
    try {
      await updateMutation.mutateAsync(data);
      showToast('Fee configuration saved successfully.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      showToast(axiosErr.response?.data?.message ?? 'Failed to save fee configuration.', 'error');
    }
  };

  // ── Render ────────────────────────────────────────────────────────────

  if (authLoading || isLoading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <p className="text-gray-500 text-sm">Loading fee configuration…</p>
      </div>
    );
  }

  if (role !== 'ADMIN') return null;

  return (
    <div className="max-w-lg mx-auto px-4 py-8 space-y-8">
      {/* Toast */}
      {toast && (
        <div
          className={`fixed top-4 right-4 z-50 rounded-lg px-4 py-3 text-sm text-white shadow-lg transition-all ${
            toast.type === 'success' ? 'bg-green-600' : 'bg-red-600'
          }`}
        >
          {toast.message}
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Fee Configuration</h1>
          <p className="mt-1 text-sm text-gray-500">
            Set clearance fees. Changes apply to all new clearance requests.
          </p>
        </div>
        <Link
          href="/backoffice/admin/settings"
          className="text-sm text-blue-600 hover:underline"
        >
          ← Barangay Settings
        </Link>
      </div>

      {/* Info banner */}
      <div className="rounded-lg border border-yellow-200 bg-yellow-50 px-4 py-3 text-sm text-yellow-800">
        <strong>Note:</strong> Existing clearance requests keep their original fee amount.
        Only new requests will use the updated fees.
      </div>

      {/* Fee form */}
      <section className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Standard Fee (₱)
            </label>
            <p className="text-xs text-gray-500 mb-2">
              Applied to STANDARD urgency clearance requests.
            </p>
            <input
              type="number"
              step="0.01"
              min="0"
              {...register('standardFee', { valueAsNumber: true })}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. 50.00"
            />
            {errors.standardFee && (
              <p className="mt-1 text-xs text-red-600">{errors.standardFee.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Rush Fee (₱)
            </label>
            <p className="text-xs text-gray-500 mb-2">
              Applied to RUSH urgency clearance requests.
            </p>
            <input
              type="number"
              step="0.01"
              min="0"
              {...register('rushFee', { valueAsNumber: true })}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. 100.00"
            />
            {errors.rushFee && (
              <p className="mt-1 text-xs text-red-600">{errors.rushFee.message}</p>
            )}
          </div>

          {/* Current values summary */}
          {feeConfig && (
            <div className="rounded-md bg-gray-50 border border-gray-200 px-4 py-3 text-sm text-gray-600">
              <p className="font-medium mb-1">Current values:</p>
              <p>Standard: ₱{feeConfig.standardFee.toFixed(2)}</p>
              <p>Rush: ₱{feeConfig.rushFee.toFixed(2)}</p>
            </div>
          )}

          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting || updateMutation.isPending}
              className="rounded-md bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {isSubmitting || updateMutation.isPending ? 'Saving…' : 'Save Fees'}
            </button>
          </div>
        </form>
      </section>

      {/* Last updated */}
      {feeConfig?.updatedAt && (
        <p className="text-xs text-gray-400">
          Last updated:{' '}
          {new Date(feeConfig.updatedAt).toLocaleString('en-PH', {
            dateStyle: 'medium',
            timeStyle: 'short',
          })}
        </p>
      )}
    </div>
  );
}
