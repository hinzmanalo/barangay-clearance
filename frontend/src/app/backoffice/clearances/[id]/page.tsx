'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import ActionButtons from '@/components/backoffice/ActionButtons';
import { useClearance, downloadClearancePdf } from '@/hooks/useClearances';
import type { ClearanceRequest } from '@/types/clearance';
import { STATUS_LABELS, PURPOSE_LABELS, PAYMENT_STATUS_LABELS } from '@/types/clearance';
import { DetailPageSkeleton } from '@/components/shared/LoadingSkeleton';
import { toast } from '@/components/shared/ErrorToast';

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  FOR_APPROVAL: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  RELEASED: 'bg-blue-100 text-blue-800',
};

const PAYMENT_COLORS: Record<string, string> = {
  UNPAID: 'bg-orange-100 text-orange-800',
  PAID: 'bg-green-100 text-green-800',
  WAIVED: 'bg-gray-100 text-gray-700',
};

export default function ClearanceDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const { data: initialCr, isLoading } = useClearance(id);
  const [cr, setCr] = useState<ClearanceRequest | null>(null);
  const [downloading, setDownloading] = useState(false);

  // Use local state override after actions, fall back to query data
  const clearance = cr ?? initialCr;

  if (isLoading) {
    return <DetailPageSkeleton />;
  }

  if (!clearance) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-8 text-sm text-red-600">Request not found.</div>
    );
  }

  const purposeLabel =
    clearance.purpose === 'OTHER' && clearance.purposeOther
      ? clearance.purposeOther
      : PURPOSE_LABELS[clearance.purpose] ?? clearance.purpose;

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/backoffice/clearances" className="text-sm text-gray-500 hover:text-gray-800">
          ← Clearances
        </Link>
        <span className="text-gray-300">/</span>
        <h1 className="text-xl font-bold text-gray-900">
          {clearance.residentName ?? 'Clearance Request'}
        </h1>
      </div>

      {/* Status + clearance number */}
      <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <span className={`inline-flex rounded-full px-3 py-1 text-sm font-medium ${
              STATUS_COLORS[clearance.status] ?? 'bg-gray-100 text-gray-700'
            }`}>
              {STATUS_LABELS[clearance.status]}
            </span>
            <span className={`inline-flex rounded-full px-3 py-1 text-sm font-medium ${
              PAYMENT_COLORS[clearance.paymentStatus] ?? 'bg-gray-100 text-gray-700'
            }`}>
              {PAYMENT_STATUS_LABELS[clearance.paymentStatus]}
            </span>
          </div>
          {clearance.clearanceNumber && (
            <span className="font-mono text-sm font-semibold text-gray-700">
              #{clearance.clearanceNumber}
            </span>
          )}
        </div>

        {/* Staff action buttons */}
        <ActionButtons clearance={clearance} onSuccess={setCr} />

        {/* Download PDF — shown when RELEASED */}
        {clearance.status === 'RELEASED' && clearance.clearanceNumber && (
          <div className="mt-4 flex items-center gap-3">
            <button
              onClick={async () => {
                setDownloading(true);
                try {
                  await downloadClearancePdf(clearance.id, clearance.clearanceNumber!);
                } catch {
                  toast.error('Failed to download PDF. Please try again.');
                } finally {
                  setDownloading(false);
                }
              }}
              disabled={downloading}
              className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              {downloading ? 'Downloading…' : 'Print / Download PDF'}
            </button>
          </div>
        )}
      </div>

      {/* Details */}
      <div className="bg-white rounded-lg shadow border border-gray-200 p-6">
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">Details</h2>
        <dl className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
          <div>
            <dt className="text-gray-500">Resident</dt>
            <dd className="font-medium text-gray-900">{clearance.residentName ?? '—'}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Purpose</dt>
            <dd className="font-medium text-gray-900">{purposeLabel}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Urgency</dt>
            <dd className="font-medium text-gray-900">{clearance.urgency}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Copies</dt>
            <dd className="font-medium text-gray-900">{clearance.copies}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Fee</dt>
            <dd className="font-medium text-gray-900">₱{Number(clearance.feeAmount).toFixed(2)}</dd>
          </div>
          <div>
            <dt className="text-gray-500">Date Submitted</dt>
            <dd className="font-medium text-gray-900">
              {new Date(clearance.createdAt).toLocaleDateString('en-PH', {
                year: 'numeric', month: 'short', day: 'numeric',
              })}
            </dd>
          </div>
          {clearance.reviewedAt && (
            <div>
              <dt className="text-gray-500">Date Reviewed</dt>
              <dd className="font-medium text-gray-900">
                {new Date(clearance.reviewedAt).toLocaleDateString('en-PH', {
                  year: 'numeric', month: 'short', day: 'numeric',
                })}
              </dd>
            </div>
          )}
          {clearance.issuedAt && (
            <div>
              <dt className="text-gray-500">Date Released</dt>
              <dd className="font-medium text-gray-900">
                {new Date(clearance.issuedAt).toLocaleDateString('en-PH', {
                  year: 'numeric', month: 'short', day: 'numeric',
                })}
              </dd>
            </div>
          )}
          {clearance.notes && (
            <div className="col-span-2">
              <dt className="text-gray-500">Notes</dt>
              <dd className="mt-1 text-gray-900 whitespace-pre-wrap">{clearance.notes}</dd>
            </div>
          )}
        </dl>
      </div>

      {/* Link to resident profile */}
      <div className="text-sm text-gray-500">
        <Link
          href={`/backoffice/residents/${clearance.residentId}`}
          className="text-blue-600 hover:underline"
        >
          View resident profile →
        </Link>
      </div>
    </div>
  );
}
