'use client';

import { useState } from 'react';
import { useReports } from '@/hooks/useReports';
import type {
  ClearanceStatus,
  ClearancePaymentStatus,
  Purpose,
} from '@/types/clearance';
import {
  STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  PURPOSE_LABELS,
} from '@/types/clearance';

// ── Helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso?: string) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-PH', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function statusBadge(status: ClearanceStatus) {
  const colors: Record<ClearanceStatus, string> = {
    DRAFT: 'bg-gray-100 text-gray-700',
    FOR_APPROVAL: 'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
    RELEASED: 'bg-blue-100 text-blue-800',
  };
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${colors[status]}`}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}

function paymentBadge(ps: ClearancePaymentStatus) {
  const colors: Record<ClearancePaymentStatus, string> = {
    UNPAID: 'bg-orange-100 text-orange-800',
    PAID: 'bg-green-100 text-green-800',
    WAIVED: 'bg-purple-100 text-purple-800',
  };
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${colors[ps]}`}
    >
      {PAYMENT_STATUS_LABELS[ps]}
    </span>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────

export default function ReportsPage() {
  // Filter form state
  const [status, setStatus] = useState<ClearanceStatus | ''>('');
  const [paymentStatus, setPaymentStatus] = useState<ClearancePaymentStatus | ''>('');
  const [purpose, setPurpose] = useState<Purpose | ''>('');
  const [purok, setPurok] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useReports({
    status: status || undefined,
    paymentStatus: paymentStatus || undefined,
    purpose: purpose || undefined,
    purok: purok || undefined,
    from: from || undefined,
    to: to || undefined,
    page,
    size: 20,
  });

  const rows = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  function resetFilters() {
    setStatus('');
    setPaymentStatus('');
    setPurpose('');
    setPurok('');
    setFrom('');
    setTo('');
    setPage(0);
  }

  const hasActiveFilters =
    status || paymentStatus || purpose || purok || from || to;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Clearance Issuance Report</h1>
          <p className="mt-1 text-sm text-gray-500">
            Digital log-book of all clearance requests
          </p>
        </div>
        {!isLoading && (
          <p className="text-sm text-gray-500">
            {totalElements.toLocaleString()} record{totalElements !== 1 ? 's' : ''}
          </p>
        )}
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border border-gray-200 px-4 py-4 space-y-3">
        <p className="text-sm font-medium text-gray-700">Filters</p>

        <div className="flex flex-wrap gap-3 items-end">
          {/* Status */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Status</label>
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as ClearanceStatus | '');
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Status</option>
              <option value="DRAFT">Draft</option>
              <option value="FOR_APPROVAL">For Approval</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
              <option value="RELEASED">Released</option>
            </select>
          </div>

          {/* Payment status */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Payment</label>
            <select
              value={paymentStatus}
              onChange={(e) => {
                setPaymentStatus(e.target.value as ClearancePaymentStatus | '');
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Payment</option>
              <option value="UNPAID">Unpaid</option>
              <option value="PAID">Paid</option>
              <option value="WAIVED">Waived</option>
            </select>
          </div>

          {/* Purpose */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Purpose</label>
            <select
              value={purpose}
              onChange={(e) => {
                setPurpose(e.target.value as Purpose | '');
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Purposes</option>
              {(Object.keys(PURPOSE_LABELS) as Purpose[]).map((p) => (
                <option key={p} value={p}>
                  {PURPOSE_LABELS[p]}
                </option>
              ))}
            </select>
          </div>

          {/* Purok / address search */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">Purok / Address</label>
            <input
              type="text"
              placeholder="e.g. Purok 3"
              value={purok}
              onChange={(e) => {
                setPurok(e.target.value);
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm w-36 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Date range */}
          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">From</label>
            <input
              type="date"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs text-gray-500">To</label>
            <input
              type="date"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                setPage(0);
              }}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Reset */}
          {hasActiveFilters && (
            <button
              onClick={resetFilters}
              className="self-end text-sm text-gray-500 hover:text-gray-800 underline pb-1.5"
            >
              Reset
            </button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center py-16">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
          </div>
        ) : isError ? (
          <p className="py-12 text-center text-sm text-red-500">
            Failed to load report. Please try again.
          </p>
        ) : rows.length === 0 ? (
          <p className="py-12 text-center text-sm text-gray-500">
            No records found for the selected filters.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Clearance No.
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Resident
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Purpose
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Urgency
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Status
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Payment
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Issued At
                  </th>
                  <th className="px-4 py-3 text-left font-medium text-gray-500 uppercase tracking-wide text-xs">
                    Created At
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((row, idx) => (
                  <tr key={idx} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-xs text-gray-800">
                      {row.clearanceNumber ?? '—'}
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {row.residentFullName}
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {PURPOSE_LABELS[row.purpose]}
                    </td>
                    <td className="px-4 py-3 text-gray-600 capitalize">
                      {row.urgency.toLowerCase()}
                    </td>
                    <td className="px-4 py-3">{statusBadge(row.status)}</td>
                    <td className="px-4 py-3">{paymentBadge(row.paymentStatus)}</td>
                    <td className="px-4 py-3 text-gray-500">{formatDate(row.issuedAt)}</td>
                    <td className="px-4 py-3 text-gray-500">{formatDate(row.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

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
