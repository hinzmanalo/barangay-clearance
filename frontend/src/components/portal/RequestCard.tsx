'use client';

import Link from 'next/link';
import type { ClearanceRequest } from '@/types/clearance';
import { STATUS_LABELS, PURPOSE_LABELS, PAYMENT_STATUS_LABELS } from '@/types/clearance';

interface RequestCardProps {
  request: ClearanceRequest;
}

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  FOR_APPROVAL: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  RELEASED: 'bg-blue-100 text-blue-800',
};

/**
 * Compact card showing a clearance request summary for the portal dashboard.
 */
export default function RequestCard({ request }: RequestCardProps) {
  const purposeLabel =
    request.purpose === 'OTHER' && request.purposeOther
      ? request.purposeOther
      : PURPOSE_LABELS[request.purpose] ?? request.purpose;

  return (
    <Link
      href={`/portal/requests/${request.id}`}
      className="block bg-white rounded-lg shadow-sm border border-gray-200 p-4 hover:shadow-md transition-shadow"
    >
      <div className="flex items-center justify-between gap-4">
        <div className="flex-1 min-w-0">
          <p className="text-sm text-gray-500 mb-0.5">
            {new Date(request.createdAt).toLocaleDateString('en-PH', {
              year: 'numeric',
              month: 'short',
              day: 'numeric',
            })}
          </p>
          <p className="font-medium text-gray-900 truncate">
            {purposeLabel}
          </p>
          {request.clearanceNumber && (
            <p className="text-xs text-gray-500 mt-0.5 font-mono">
              #{request.clearanceNumber}
            </p>
          )}
        </div>

        <div className="flex flex-col items-end gap-1.5">
          <span
            className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
              STATUS_COLORS[request.status] ?? 'bg-gray-100 text-gray-700'
            }`}
          >
            {STATUS_LABELS[request.status]}
          </span>
          <span className="text-xs text-gray-500">
            {PAYMENT_STATUS_LABELS[request.paymentStatus]} · ₱{Number(request.feeAmount).toFixed(2)}
          </span>
        </div>
      </div>
    </Link>
  );
}
