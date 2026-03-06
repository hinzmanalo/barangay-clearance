'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import { useCreateWalkInClearance } from '@/hooks/useClearances';
import { useResidents } from '@/hooks/useResidents';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { Button } from '@/components/ui/Button';
import type { Purpose, Urgency } from '@/types/clearance';
import { PURPOSE_LABELS } from '@/types/clearance';
import { AxiosError } from 'axios';

const schema = z
  .object({
    residentId: z.string().uuid('Select a resident'),
    purpose: z.enum(
      ['EMPLOYMENT', 'TRAVEL_ABROAD', 'SCHOLARSHIP', 'LOAN', 'BUSINESS_PERMIT', 'LEGAL', 'CEDULA', 'OTHER'] as const
    ),
    purposeOther: z.string().max(255).optional(),
    urgency: z.enum(['STANDARD', 'RUSH'] as const),
    copies: z.number().min(1).max(10),
    notes: z.string().max(1000).optional(),
  })
  .refine(
    (data) => data.purpose !== 'OTHER' || (data.purposeOther && data.purposeOther.trim().length > 0),
    { message: 'Please describe the purpose', path: ['purposeOther'] }
  );

type FormData = z.infer<typeof schema>;

export default function NewWalkInClearancePage() {
  const router = useRouter();
  const createMutation = useCreateWalkInClearance();
  const [serverError, setServerError] = useState<string | null>(null);
  const [residentSearch, setResidentSearch] = useState('');
  const [selectedResidentDisplay, setSelectedResidentDisplay] = useState<string | null>(null);

  const { data: residentsPage } = useResidents({ q: residentSearch && !selectedResidentDisplay ? residentSearch : '', size: 10 });
  const residents = residentsPage?.content ?? [];

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { copies: 1, urgency: 'STANDARD' },
  });

  const purpose = watch('purpose');
  const residentId = watch('residentId');

  const onSubmit = async (data: FormData) => {
    setServerError(null);
    try {
      const result = await createMutation.mutateAsync({
        residentId: data.residentId,
        purpose: data.purpose as Purpose,
        purposeOther: data.purposeOther || undefined,
        urgency: data.urgency as Urgency,
        copies: data.copies,
        notes: data.notes || undefined,
      });
      router.push(`/backoffice/clearances/${result.id}`);
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Failed to create request.');
    }
  };

  return (
    <div className="mx-auto max-w-2xl px-4 py-8 space-y-8">
      {/* Page Header */}
      <PageHeader title="New Walk-in Request" backHref="/backoffice/clearances" />

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)}>
        <Card className="p-6 space-y-6">
          {/* Server error */}
          {serverError && (
            <div className="rounded-lg bg-red-50 border border-red-200 p-4">
              <p className="font-geist text-sm text-red-700">{serverError}</p>
            </div>
          )}

          {/* Resident search + select */}
          <div>
            <label className="block font-geist text-sm font-medium text-neutral-900 mb-2">
              Resident <span className="text-red-600">*</span>
            </label>
            {selectedResidentDisplay ? (
              <div className="flex items-center gap-3 mb-2">
                <div className="flex-1 px-3 py-2 rounded-lg border border-green-300 bg-green-50">
                  <p className="font-geist text-sm text-green-900 font-medium">{selectedResidentDisplay}</p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setValue('residentId', '');
                    setSelectedResidentDisplay(null);
                    setResidentSearch('');
                  }}
                  className="px-3 py-2 text-xs font-medium text-neutral-600 hover:text-neutral-900 border border-neutral-300 rounded-lg hover:bg-neutral-50"
                >
                  Clear
                </button>
              </div>
            ) : (
              <>
                <Input
                  type="text"
                  value={residentSearch}
                  onChange={(e) => setResidentSearch(e.target.value)}
                  placeholder="Type to search resident name…"
                  className="mb-2"
                />
                {residents.length > 0 && (
                  <div className="border border-neutral-200 rounded-lg divide-y max-h-48 overflow-y-auto">
                    {residents.map((r) => (
                      <button
                        key={r.id}
                        type="button"
                        onClick={() => {
                          setValue('residentId', r.id);
                          setSelectedResidentDisplay(`${r.lastName}, ${r.firstName}`);
                          setResidentSearch('');
                        }}
                        className="w-full text-left px-4 py-2 text-sm hover:bg-blue-50 transition-colors text-neutral-700"
                      >
                        {r.lastName}, {r.firstName} {r.middleName ? r.middleName[0] + '.' : ''}
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}
            <input type="hidden" {...register('residentId')} />
            {errors.residentId && (
              <p className="mt-2 font-geist text-xs text-red-600">{errors.residentId.message}</p>
            )}
          </div>

          {/* Purpose */}
          <div>
            <label className="block font-geist text-sm font-medium text-neutral-900 mb-2">Purpose <span className="text-red-600">*</span></label>
            <Select {...register('purpose')} error={errors.purpose?.message}>
              <option value="">Select purpose…</option>
              {(Object.entries(PURPOSE_LABELS) as [Purpose, string][]).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </div>

          {/* Purpose description (if OTHER selected) */}
          {purpose === 'OTHER' && (
            <div>
              <label className="block font-geist text-sm font-medium text-neutral-900 mb-2">Purpose description</label>
              <Input
                placeholder="Please describe the purpose…"
                {...register('purposeOther')}
                error={errors.purposeOther?.message}
              />
            </div>
          )}

          {/* Urgency */}
          <div>
            <label className="block font-geist text-sm font-medium text-neutral-900 mb-3">
              Urgency <span className="text-red-600">*</span>
            </label>
            <div className="flex gap-6">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  {...register('urgency')}
                  value="STANDARD"
                  className="w-4 h-4 accent-blue-600"
                />
                <span className="font-geist text-sm text-neutral-700">Standard (₱50.00)</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  {...register('urgency')}
                  value="RUSH"
                  className="w-4 h-4 accent-blue-600"
                />
                <span className="font-geist text-sm text-neutral-700">Rush (₱100.00)</span>
              </label>
            </div>
            {errors.urgency && (
              <p className="mt-2 font-geist text-xs text-red-600">{errors.urgency.message}</p>
            )}
          </div>

          {/* Copies */}
          <div>
            <label className="block font-geist text-sm font-medium text-neutral-900 mb-2">Number of Copies <span className="text-red-600">*</span></label>
            <Input
              type="number"
              min={1}
              max={10}
              {...register('copies', { valueAsNumber: true })}
              error={errors.copies?.message}
              className="w-32"
            />
          </div>

          {/* Notes */}
          <div>
            <label className="block font-geist text-sm font-medium text-neutral-900 mb-2">Notes (optional)</label>
            <Textarea
              placeholder="Add any notes for staff…"
              rows={4}
              {...register('notes')}
              error={errors.notes?.message}
            />
          </div>

          {/* Submit button */}
          <Button
            variant="primary"
            size="lg"
            className="w-full"
            loading={isSubmitting}
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Creating…' : 'Create Request'}
          </Button>
        </Card>
      </form>
    </div>
  );
}
