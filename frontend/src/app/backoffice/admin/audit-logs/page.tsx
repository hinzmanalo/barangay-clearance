'use client';

import { useState, useEffect } from 'react';
import AuditLogTable from '@/components/backoffice/AuditLogTable';
import { useAuditLogs } from '@/hooks/useAuditLogs';
import type { AuditLog } from '@/types/audit';

const ACTION_OPTIONS = [
  '',
  // Identity
  'USER_REGISTERED',
  'USER_LOGIN',
  'USER_LOGIN_FAILED',
  'USER_LOGOUT',
  'USER_TOKEN_REFRESHED',
  'USER_PASSWORD_CHANGED',
  // Staff
  'STAFF_CREATED',
  'STAFF_ACTIVATED',
  'STAFF_DEACTIVATED',
  'STAFF_ROLE_CHANGED',
  'STAFF_PASSWORD_RESET',
  // Residents
  'RESIDENT_CREATED',
  'RESIDENT_UPDATED',
  'RESIDENT_ACTIVATED',
  // Clearance
  'CLEARANCE_SUBMITTED',
  'CLEARANCE_APPROVED',
  'CLEARANCE_REJECTED',
  'CLEARANCE_RESUBMITTED',
  'CLEARANCE_RELEASED',
  'CLEARANCE_PDF_DOWNLOADED',
  // Payments
  'PAYMENT_INITIATED',
  'PAYMENT_SUCCESS',
  'PAYMENT_FAILED',
  'PAYMENT_CASH_RECORDED',
  // Settings
  'SETTINGS_UPDATED',
  'SETTINGS_LOGO_UPLOADED',
  'FEES_UPDATED',
] as const;

const ENTITY_TYPE_OPTIONS = [
  '',
  'User',
  'Resident',
  'ClearanceRequest',
  'Payment',
  'BarangaySettings',
  'FeeConfig',
] as const;

export default function AuditLogsPage() {
  const [action, setAction] = useState('');
  const [entityType, setEntityType] = useState('');
  const [userIdInput, setUserIdInput] = useState('');
  const [debouncedUserId, setDebouncedUserId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);

  // Debounce user ID input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedUserId(userIdInput);
      setPage(0);
    }, 400);
    return () => clearTimeout(timer);
  }, [userIdInput]);

  // Reset page on filter changes
  useEffect(() => {
    setPage(0);
  }, [action, entityType, from, to]);

  const { data, isLoading } = useAuditLogs({
    action: action || undefined,
    entityType: entityType || undefined,
    userId: debouncedUserId || undefined,
    from: from ? new Date(from).toISOString() : undefined,
    to: to ? new Date(to).toISOString() : undefined,
    page,
    size: 20,
  });

  const logs: AuditLog[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Audit Logs</h1>
        <p className="mt-1 text-sm text-gray-500">
          System-wide record of all state-changing operations. Click a row to expand details.
        </p>
      </div>

      {/* Filters */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        {/* Action filter */}
        <select
          value={action}
          onChange={(e) => setAction(e.target.value)}
          className="block rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option value="">All actions</option>
          {ACTION_OPTIONS.filter(Boolean).map((a) => (
            <option key={a} value={a}>{a}</option>
          ))}
        </select>

        {/* Entity type filter */}
        <select
          value={entityType}
          onChange={(e) => setEntityType(e.target.value)}
          className="block rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        >
          <option value="">All entity types</option>
          {ENTITY_TYPE_OPTIONS.filter(Boolean).map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>

        {/* Actor user ID filter */}
        <input
          type="text"
          placeholder="Actor User ID (UUID)…"
          value={userIdInput}
          onChange={(e) => setUserIdInput(e.target.value)}
          className="block rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />

        {/* From date */}
        <div className="flex flex-col gap-0.5">
          <label className="text-[11px] text-gray-500 font-medium">From</label>
          <input
            type="datetime-local"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="block rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        {/* To date */}
        <div className="flex flex-col gap-0.5">
          <label className="text-[11px] text-gray-500 font-medium">To</label>
          <input
            type="datetime-local"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="block rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        {/* Clear filters */}
        <button
          type="button"
          onClick={() => {
            setAction('');
            setEntityType('');
            setUserIdInput('');
            setFrom('');
            setTo('');
            setPage(0);
          }}
          className="self-end rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
        >
          Clear filters
        </button>
      </div>

      {/* Table */}
      <AuditLogTable logs={logs} isLoading={isLoading} />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-200 pt-4">
          <p className="text-sm text-gray-600">
            Page {page + 1} of {totalPages} &middot; {data?.totalElements ?? 0} total
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40"
            >
              Previous
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
