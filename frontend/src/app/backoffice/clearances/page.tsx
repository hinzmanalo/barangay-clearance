'use client';

import { useState } from 'react';
import Link from 'next/link';
import ClearanceTable from '@/components/backoffice/ClearanceTable';
import { useClearances, useClearanceSummary } from '@/hooks/useClearances';
import type { ClearanceStatus, ClearancePaymentStatus } from '@/types/clearance';

export default function ClearancesPage() {
  const [status, setStatus] = useState<ClearanceStatus | ''>('');
  const [paymentStatus, setPaymentStatus] = useState<ClearancePaymentStatus | ''>('');
  const [page, setPage] = useState(0);

  const { data, isLoading } = useClearances({
    status: status || undefined,
    paymentStatus: paymentStatus || undefined,
    page,
    size: 20,
  });

  const { data: summary } = useClearanceSummary();
  const clearances = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Clearance Requests</h1>
        <Link
          href="/backoffice/clearances/new"
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + Walk-in Request
        </Link>
      </div>

      {/* Summary cards */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="bg-white rounded-lg border border-yellow-200 p-4">
            <p className="text-xs text-yellow-600 font-medium uppercase tracking-wide">Pending Approval</p>
            <p className="mt-1 text-3xl font-bold text-yellow-700">{summary.pendingApproval}</p>
          </div>
          <div className="bg-white rounded-lg border border-green-200 p-4">
            <p className="text-xs text-green-600 font-medium uppercase tracking-wide">Approved</p>
            <p className="mt-1 text-3xl font-bold text-green-700">{summary.approved}</p>
          </div>
          <div className="bg-white rounded-lg border border-orange-200 p-4">
            <p className="text-xs text-orange-600 font-medium uppercase tracking-wide">Awaiting Payment</p>
            <p className="mt-1 text-3xl font-bold text-orange-700">{summary.awaitingPayment}</p>
          </div>
          <div className="bg-white rounded-lg border border-blue-200 p-4">
            <p className="text-xs text-blue-600 font-medium uppercase tracking-wide">Released Today</p>
            <p className="mt-1 text-3xl font-bold text-blue-700">{summary.releasedToday}</p>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-center bg-white rounded-lg border border-gray-200 px-4 py-3">
        <span className="text-sm font-medium text-gray-600">Filter:</span>

        <select
          value={status}
          onChange={(e) => { setStatus(e.target.value as ClearanceStatus | ''); setPage(0); }}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All Status</option>
          <option value="FOR_APPROVAL">For Approval</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="RELEASED">Released</option>
        </select>

        <select
          value={paymentStatus}
          onChange={(e) => { setPaymentStatus(e.target.value as ClearancePaymentStatus | ''); setPage(0); }}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All Payment</option>
          <option value="UNPAID">Unpaid</option>
          <option value="PAID">Paid</option>
        </select>

        {(status || paymentStatus) && (
          <button
            onClick={() => { setStatus(''); setPaymentStatus(''); setPage(0); }}
            className="text-sm text-gray-500 hover:text-gray-800 underline"
          >
            Clear
          </button>
        )}
      </div>

      {/* Table */}
      <ClearanceTable clearances={clearances} isLoading={isLoading} />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded border border-gray-300 px-3 py-1 text-sm disabled:opacity-40 hover:bg-gray-50"
          >
            Previous
          </button>
          <span className="text-sm text-gray-500">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="rounded border border-gray-300 px-3 py-1 text-sm disabled:opacity-40 hover:bg-gray-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
