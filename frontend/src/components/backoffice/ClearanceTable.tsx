'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import type { ClearanceRequest } from '@/types/clearance';
import { STATUS_LABELS, PURPOSE_LABELS, PAYMENT_STATUS_LABELS } from '@/types/clearance';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { staggerContainer, staggerItem } from '@/lib/animations';

interface ClearanceTableProps {
  clearances: ClearanceRequest[];
  isLoading?: boolean;
}

/**
 * Backoffice table listing clearance requests with staggered row entrance.
 */
export default function ClearanceTable({ clearances, isLoading }: ClearanceTableProps) {
  if (isLoading) {
    return (
      <div className="overflow-x-auto bg-white rounded-lg border border-neutral-200">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-neutral-100 bg-neutral-50">
              {['Resident', 'Purpose', 'Urgency', 'Status', 'Payment', 'Date', 'Clearance #'].map((h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: 5 }).map((_, i) => (
              <tr key={i} className="border-b border-neutral-50">
                {Array.from({ length: 7 }).map((_, j) => (
                  <td key={j} className="px-4 py-3">
                    <Skeleton className="h-4 w-20" />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (clearances.length === 0) {
    return (
      <div className="bg-white rounded-lg border border-neutral-200 p-8 text-center">
        <p className="text-sm text-neutral-500">No clearance requests found.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto bg-white rounded-lg border border-neutral-200">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-neutral-100 bg-neutral-50">
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Resident
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Purpose
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Urgency
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Status
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Payment
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Date
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Clearance #
            </th>
          </tr>
        </thead>
        <tbody>
          <motion.tr
            className="contents"
            variants={staggerContainer}
            initial="hidden"
            animate="visible"
          >
            {clearances.map((cr) => {
              const purposeLabel =
                cr.purpose === 'OTHER' && cr.purposeOther
                  ? cr.purposeOther
                  : PURPOSE_LABELS[cr.purpose] ?? cr.purpose;

              return (
                <motion.tr
                  key={cr.id}
                  className="border-b border-neutral-50 hover:bg-neutral-50 transition-colors"
                  variants={staggerItem}
                >
                  <td className="px-4 py-3">
                    <Link
                      href={`/backoffice/clearances/${cr.id}`}
                      className="font-medium text-blue-600 hover:text-blue-700 transition-colors"
                    >
                      {cr.residentName ?? '—'}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-neutral-700 max-w-[180px] truncate">{purposeLabel}</td>
                  <td className="px-4 py-3 text-neutral-600">{cr.urgency}</td>
                  <td className="px-4 py-3">
                    <Badge variant="status" value={cr.status} dot />
                  </td>
                  <td className="px-4 py-3">
                    <Badge variant="payment" value={cr.paymentStatus} dot />
                  </td>
                  <td className="px-4 py-3 text-neutral-500 text-xs whitespace-nowrap">
                    {new Date(cr.createdAt).toLocaleDateString('en-PH', {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </td>
                  <td className="px-4 py-3 font-mono text-neutral-600 text-xs">
                    {cr.clearanceNumber ?? '—'}
                  </td>
                </motion.tr>
              );
            })}
          </motion.tr>
        </tbody>
      </table>
    </div>
  );
}
