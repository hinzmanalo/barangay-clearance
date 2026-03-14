'use client';

import { useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { useFeeConfig, useUpdateFeeConfig } from '@/hooks/useSettings';
import { AxiosError } from 'axios';
import { toast } from '@/components/shared/ErrorToast';
import { Skeleton } from '@/components/ui/Skeleton';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { DollarSign, Zap } from 'lucide-react';

const feesSchema = z.object({
  standardFee: z
    .number()
    .min(0, 'Standard fee must be non-negative')
    .multipleOf(0.01, 'Maximum 2 decimal places'),
  rushFee: z
    .number()
    .min(0, 'Rush fee must be non-negative')
    .multipleOf(0.01, 'Maximum 2 decimal places'),
});

type FeesForm = z.infer<typeof feesSchema>;

export default function FeesPage() {
  const { role, isLoading: authLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!authLoading && role !== 'ADMIN') {
      router.replace('/backoffice/dashboard');
    }
  }, [authLoading, role, router]);

  const { data: feeConfig, isLoading } = useFeeConfig();
  const updateMutation = useUpdateFeeConfig();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FeesForm>({
    resolver: zodResolver(feesSchema),
  });

  useEffect(() => {
    if (feeConfig) {
      reset({
        standardFee: feeConfig.standardFee,
        rushFee: feeConfig.rushFee,
      });
    }
  }, [feeConfig, reset]);

  const showToast = useCallback((message: string, type: 'success' | 'error') => {
    if (type === 'success') toast.success(message);
    else toast.error(message);
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

  if (authLoading || isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8 space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-48 rounded-lg" />
          <Skeleton className="h-48 rounded-lg" />
        </div>
      </div>
    );
  }

 if (role !== 'ADMIN') return null;

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 space-y-8">
      <PageHeader
        title="Fee Configuration"
        description="Set clearance fees. Changes apply to all new clearance requests."
        backHref="/backoffice/admin/settings"
      />

      <Card accentColor="amber" className="p-4">
        <p className="text-sm font-medium text-neutral-800">
          Existing clearance requests keep their original fee amount. Only new requests will use the updated fees.
        </p>
      </Card>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card accentColor="blue" className="p-6">
            <div className="flex items-start gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
                <DollarSign className="w-5 h-5 text-blue-600" />
              </div>
              <div>
                <h3 className="font-semibold text-neutral-900">Standard Fee</h3>
                <p className="text-xs text-neutral-500">For STANDARD urgency requests</p>
              </div>
            </div>

            <div className="mb-4">
              <Input
                type="number"
                step="0.01"
                min="0"
                {...register('standardFee', { valueAsNumber: true })}
                error={errors.standardFee?.message}
                placeholder="e.g. 50.00"
              />
            </div>

            {feeConfig && (
              <div className="text-sm text-neutral-500">
                <p className="text-xs font-medium text-neutral-400 mb-1">Current value:</p>
                <p className="text-lg font-semibold text-blue-600">
                  ₱{feeConfig.standardFee.toFixed(2)}
                </p>
              </div>
            )}
          </Card>

          <Card accentColor="teal" className="p-6">
            <div className="flex items-start gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-teal-100 flex items-center justify-center">
                <Zap className="w-5 h-5 text-teal-600" />
              </div>
              <div>
                <h3 className="font-semibold text-neutral-900">Rush Fee</h3>
                <p className="text-xs text-neutral-500">For RUSH urgency requests</p>
              </div>
            </div>

            <div className="mb-4">
              <Input
                type="number"
                step="0.01"
                min="0"
                {...register('rushFee', { valueAsNumber: true })}
                error={errors.rushFee?.message}
                placeholder="e.g. 100.00"
              />
            </div>

            {feeConfig && (
              <div className="text-sm text-neutral-500">
                <p className="text-xs font-medium text-neutral-400 mb-1">Current value:</p>
                <p className="text-lg font-semibold text-teal-600">
                  ₱{feeConfig.rushFee.toFixed(2)}
                </p>
              </div>
            )}
          </Card>
        </div>

        <div className="flex justify-end">
          <Button
            type="submit"
            variant="primary"
            loading={isSubmitting || updateMutation.isPending}
          >
            {isSubmitting || updateMutation.isPending ? 'Saving…' : 'Save Fees'}
          </Button>
        </div>
      </form>

      {feeConfig?.updatedAt && (
        <p className="text-xs text-neutral-400">
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
