'use client';

import Link from 'next/link';
import type { ClearanceRequest } from '@/types/clearance';
import { STATUS_LABELS, PURPOSE_LABELS, PAYMENT_STATUS_LABELS } from '@/types/clearance';
import { TableRowSkeleton } from '@/components/shared/LoadingSkeleton';

interface ClearanceTableProps {
  clearances: ClearanceRequest[];
  isLoading?: boolean;
}

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

/**
 * Backoffice table listing clearance requests.
 */
export default function ClearanceTable({ clearances, isLoading }: ClearanceTableProps) {
  if (isLoading) {
    return (
      <div className="overflow-x-auto bg-white rounded-lg shadow border border-gray-200">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {['Resident', 'Purpose', 'Urgency', 'Status', 'Payment', 'Date', 'Clearance #'].map((h) => (
                <th key={h} className="px-4 py-3 text-left font-semibold text-gray-600">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {Array.from({ length: 5 }).map((_, i) => (
              <TableRowSkeleton key={i} cols={7} />
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (clearances.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow border border-gray-200 p-8 text-center text-sm text-gray-500">
        No clearance requests found.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto bg-white rounded-lg shadow border border-gray-200">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Resident</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Purpose</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Urgency</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Status</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Payment</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Date</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Clearance #</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {clearances.map((cr) => {
            const purposeLabel =
              cr.purpose === 'OTHER' && cr.purposeOther
                ? cr.purposeOther
                : PURPOSE_LABELS[cr.purpose] ?? cr.purpose;

            return (
              <tr key={cr.id} className="hover:bg-gray-50">
                <td className="px-4 py-3">
                  <Link
                    href={`/backoffice/clearances/${cr.id}`}
                    className="font-medium text-blue-600 hover:underline"
                  >
                    {cr.residentName ?? '—'}
                  </Link>
                </td>
                <td className="px-4 py-3 text-gray-700 max-w-[180px] truncate">{purposeLabel}</td>
                <td className="px-4 py-3 text-gray-500">{cr.urgency}</td>
                <td className="px-4 py-3">
                  <span
                    className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      STATUS_COLORS[cr.status] ?? 'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {STATUS_LABELS[cr.status]}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span
                    className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${
                      PAYMENT_COLORS[cr.paymentStatus] ?? 'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {PAYMENT_STATUS_LABELS[cr.paymentStatus]}
                  </span>
                </td>
                <td className="px-4 py-3 text-gray-500 whitespace-nowrap">
                  {new Date(cr.createdAt).toLocaleDateString('en-PH', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </td>
                <td className="px-4 py-3 font-mono text-gray-600 text-xs">
                  {cr.clearanceNumber ?? '—'}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
