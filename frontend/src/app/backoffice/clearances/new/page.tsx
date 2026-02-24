'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useCreateWalkInClearance } from '@/hooks/useClearances';
import { useResidents } from '@/hooks/useResidents';
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

  const { data: residentsPage } = useResidents({ q: residentSearch, size: 10 });
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
  const selectedResident = residents.find((r) => r.id === residentId);

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
    <div className="max-w-xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/backoffice/clearances" className="text-sm text-gray-500 hover:text-gray-800">
          ← Clearances
        </Link>
        <span className="text-gray-300">/</span>
        <h1 className="text-2xl font-bold text-gray-900">Walk-in Request</h1>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="bg-white shadow rounded-lg p-6 space-y-5">
        {serverError && (
          <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
            {serverError}
          </div>
        )}

        {/* Resident search + select */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Resident *</label>
          <input
            type="text"
            value={residentSearch}
            onChange={(e) => setResidentSearch(e.target.value)}
            placeholder="Type to search resident name…"
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 mb-2"
          />
          {residents.length > 0 && (
            <div className="border border-gray-200 rounded-md divide-y max-h-48 overflow-y-auto">
              {residents.map((r) => (
                <button
                  key={r.id}
                  type="button"
                  onClick={() => {
                    setValue('residentId', r.id);
                    setResidentSearch(`${r.lastName}, ${r.firstName}`);
                  }}
                  className={`w-full text-left px-3 py-2 text-sm hover:bg-blue-50 ${
                    residentId === r.id ? 'bg-blue-50 font-medium text-blue-700' : 'text-gray-700'
                  }`}
                >
                  {r.lastName}, {r.firstName} {r.middleName ? r.middleName[0] + '.' : ''}
                </button>
              ))}
            </div>
          )}
          <input type="hidden" {...register('residentId')} />
          {errors.residentId && (
            <p className="mt-1 text-xs text-red-600">{errors.residentId.message}</p>
          )}
        </div>

        {/* Purpose */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Purpose *</label>
          <select
            {...register('purpose')}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Select purpose…</option>
            {(Object.entries(PURPOSE_LABELS) as [Purpose, string][]).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          {errors.purpose && <p className="mt-1 text-xs text-red-600">{errors.purpose.message}</p>}
        </div>

        {purpose === 'OTHER' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Purpose description *</label>
            <input
              type="text"
              {...register('purposeOther')}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.purposeOther && (
              <p className="mt-1 text-xs text-red-600">{errors.purposeOther.message}</p>
            )}
          </div>
        )}

        {/* Urgency */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Urgency *</label>
          <div className="flex gap-4">
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="radio" {...register('urgency')} value="STANDARD" className="accent-blue-600" />
              <span className="text-sm text-gray-700">Standard (₱50.00)</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="radio" {...register('urgency')} value="RUSH" className="accent-blue-600" />
              <span className="text-sm text-gray-700">Rush (₱100.00)</span>
            </label>
          </div>
          {errors.urgency && <p className="mt-1 text-xs text-red-600">{errors.urgency.message}</p>}
        </div>

        {/* Copies */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Number of Copies</label>
          <input
            type="number"
            min={1}
            max={10}
            {...register('copies', { valueAsNumber: true })}
            className="w-24 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* Notes */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Notes (optional)</label>
          <textarea
            rows={3}
            {...register('notes')}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-md bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Creating…' : 'Create Request'}
        </button>
      </form>
    </div>
  );
}
