'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import StatusTimeline from '@/components/portal/StatusTimeline';
import { useMyClearance, useResubmitClearance } from '@/hooks/useClearances';
import type { Purpose, Urgency } from '@/types/clearance';
import { STATUS_LABELS, PURPOSE_LABELS, PAYMENT_STATUS_LABELS } from '@/types/clearance';
import { AxiosError } from 'axios';

const resubmitSchema = z
  .object({
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
    { message: 'Please describe your purpose', path: ['purposeOther'] }
  );

type ResubmitForm = z.infer<typeof resubmitSchema>;

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  FOR_APPROVAL: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  RELEASED: 'bg-blue-100 text-blue-800',
};

export default function PortalRequestDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const { data: cr, isLoading, refetch } = useMyClearance(id);
  const resubmitMutation = useResubmitClearance();
  const [showResubmit, setShowResubmit] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ResubmitForm>({
    resolver: zodResolver(resubmitSchema),
  });

  const purpose = watch('purpose');

  const onResubmit = async (data: ResubmitForm) => {
    setServerError(null);
    try {
      await resubmitMutation.mutateAsync({
        id,
        payload: {
          purpose: data.purpose as Purpose,
          purposeOther: data.purposeOther || undefined,
          urgency: data.urgency as Urgency,
          copies: data.copies,
          notes: data.notes || undefined,
        },
      });
      setShowResubmit(false);
      reset();
      refetch();
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      setServerError(axiosErr.response?.data?.message ?? 'Failed to resubmit.');
    }
  };

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8 text-sm text-gray-500">Loading…</div>
    );
  }

  if (!cr) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8 text-sm text-red-600">Request not found.</div>
    );
  }

  const purposeLabel =
    cr.purpose === 'OTHER' && cr.purposeOther
      ? cr.purposeOther
      : PURPOSE_LABELS[cr.purpose] ?? cr.purpose;

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/portal/dashboard" className="text-sm text-gray-500 hover:text-gray-800">
          ← My Requests
        </Link>
        <span className="text-gray-300">/</span>
        <h1 className="text-xl font-bold text-gray-900">Clearance Request</h1>
      </div>

      {/* Status Timeline */}
      <div className="bg-white rounded-lg shadow border border-gray-200 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <span
            className={`inline-flex rounded-full px-3 py-1 text-sm font-medium ${
              STATUS_COLORS[cr.status] ?? 'bg-gray-100 text-gray-700'
            }`}
          >
            {STATUS_LABELS[cr.status]}
          </span>
          {cr.clearanceNumber && (
            <span className="font-mono text-sm text-gray-700">#{cr.clearanceNumber}</span>
          )}
        </div>
        <StatusTimeline currentStatus={cr.status} reviewedAt={cr.reviewedAt} issuedAt={cr.issuedAt} />
      </div>

      {/* Details */}
      <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">Details</h2>
        <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
          <div>
            <dt className="text-gray-500">Purpose</dt>
            <dd className="font-medium text-gray-900">{purposeLabel}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Urgency</dt>
            <dd className="font-medium text-gray-900">{cr.urgency}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Copies</dt>
            <dd className="font-medium text-gray-900">{cr.copies}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Fee</dt>
            <dd className="font-medium text-gray-900">₱{Number(cr.feeAmount).toFixed(2)}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Payment</dt>
            <dd className="font-medium text-gray-900">{PAYMENT_STATUS_LABELS[cr.paymentStatus]}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Date Submitted</dt>
            <dd className="font-medium text-gray-900">
              {new Date(cr.createdAt).toLocaleDateString('en-PH', {
                year: 'numeric', month: 'long', day: 'numeric',
              })}
            </dd>
          </div>
          {cr.issuedAt && (
            <div className="col-span-2">
              <dt className="text-gray-500">Date Released</dt>
              <dd className="font-medium text-gray-900">
                {new Date(cr.issuedAt).toLocaleDateString('en-PH', {
                  year: 'numeric', month: 'long', day: 'numeric',
                })}
              </dd>
            </div>
          )}
          {cr.notes && (
            <div className="col-span-2">
              <dt className="text-gray-500">Notes</dt>
              <dd className="mt-1 text-gray-900 whitespace-pre-wrap">{cr.notes}</dd>
            </div>
          )}
        </dl>
      </div>

      {/* Resubmit section for rejected requests */}
      {cr.status === 'REJECTED' && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 space-y-4">
          <h2 className="text-sm font-semibold text-red-800">This request was rejected.</h2>
          <p className="text-sm text-red-700">You can resubmit it after making the necessary adjustments.</p>

          {!showResubmit ? (
            <button
              onClick={() => setShowResubmit(true)}
              className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
            >
              Resubmit Request
            </button>
          ) : (
            <form onSubmit={handleSubmit(onResubmit)} className="space-y-4">
              {serverError && (
                <div className="rounded-md bg-red-100 border border-red-300 p-3 text-sm text-red-700">
                  {serverError}
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Purpose *</label>
                <select
                  {...register('purpose')}
                  defaultValue={cr.purpose}
                  className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                >
                  {(Object.entries(PURPOSE_LABELS) as [Purpose, string][]).map(([v, l]) => (
                    <option key={v} value={v}>{l}</option>
                  ))}
                </select>
                {errors.purpose && <p className="mt-1 text-xs text-red-600">{errors.purpose.message}</p>}
              </div>

              {purpose === 'OTHER' && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Describe purpose *</label>
                  <input
                    type="text"
                    {...register('purposeOther')}
                    defaultValue={cr.purposeOther}
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none"
                  />
                  {errors.purposeOther && <p className="mt-1 text-xs text-red-600">{errors.purposeOther.message}</p>}
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                <textarea
                  rows={3}
                  {...register('notes')}
                  defaultValue={cr.notes?.replace(/^\[REJECTED\] [^\n]+\n?/, '') ?? ''}
                  className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none"
                />
              </div>

              <input type="hidden" {...register('urgency')} value={cr.urgency} />
              <input type="hidden" {...register('copies', { valueAsNumber: true })} value={cr.copies} />

              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  {isSubmitting ? 'Submitting…' : 'Resubmit'}
                </button>
                <button
                  type="button"
                  onClick={() => { setShowResubmit(false); setServerError(null); }}
                  className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      )}
    </div>
  );
}
