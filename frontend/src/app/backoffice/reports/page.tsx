'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { FileSearch } from 'lucide-react';
import { useReports } from '@/hooks/useReports';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { staggerContainer, staggerItem } from '@/lib/animations';
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
    <div className="mx-auto max-w-7xl px-4 py-8 space-y-8">
      {/* Page Header */}
      <PageHeader
        title="Clearance Issuance Report"
        description="Digital log-book of all clearance requests"
      />

      {/* Filters */}
      <Card className="p-5">
        <div className="flex flex-wrap gap-4 items-end">
          <Select
            label="Status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as ClearanceStatus | '');
              setPage(0);
            }}
          >
            <option value="">All Status</option>
            <option value="DRAFT">Draft</option>
            <option value="FOR_APPROVAL">For Approval</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="RELEASED">Released</option>
          </Select>

          <Select
            label="Payment"
            value={paymentStatus}
            onChange={(e) => {
              setPaymentStatus(e.target.value as ClearancePaymentStatus | '');
              setPage(0);
            }}
          >
            <option value="">All Payment</option>
            <option value="UNPAID">Unpaid</option>
            <option value="PAID">Paid</option>
            <option value="WAIVED">Waived</option>
          </Select>

          <Select
            label="Purpose"
            value={purpose}
            onChange={(e) => {
              setPurpose(e.target.value as Purpose | '');
              setPage(0);
            }}
          >
            <option value="">All Purposes</option>
            {(Object.keys(PURPOSE_LABELS) as Purpose[]).map((p) => (
              <option key={p} value={p}>
                {PURPOSE_LABELS[p]}
              </option>
            ))}
          </Select>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Purok / Address</label>
            <Input
              type="text"
              placeholder="e.g. Purok 3"
              value={purok}
              onChange={(e) => {
                setPurok(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">From</label>
            <Input
              type="date"
              value={from}
              onChange={(e) => {
                setFrom(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">To</label>
            <Input
              type="date"
              value={to}
              onChange={(e) => {
                setTo(e.target.value);
                setPage(0);
              }}
            />
          </div>

          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={resetFilters}>
              Clear Filters
            </Button>
          )}
        </div>
      </Card>

      {/* Results header */}
      {!isLoading && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.2 }}
          className="flex items-center justify-between"
        >
          <motion.p className="font-geist text-sm text-neutral-600">
            <span className="font-semibold text-neutral-900">
              {totalElements.toLocaleString()}
            </span>{' '}
            record{totalElements !== 1 ? 's' : ''} found
          </motion.p>
        </motion.div>
      )}

      {/* Table */}
      <Card>
        {isLoading ? (
          <div className="flex items-center justify-center py-16">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
          </div>
        ) : isError ? (
          <div className="py-12 text-center">
            <p className="text-sm text-red-600">
              Failed to load report. Please try again.
            </p>
          </div>
        ) : rows.length === 0 ? (
          <div className="py-12 text-center">
            <FileSearch className="w-8 h-8 text-neutral-300 mx-auto mb-3" />
            <p className="text-sm text-neutral-500">No records found for the selected filters.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-neutral-100 bg-neutral-50">
                  <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
                    Clearance No.
                  </th>
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
                    Issued At
                  </th>
                  <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
                    Created At
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
                  {rows.map((row, idx) => (
                    <motion.tr
                      key={idx}
                      className="border-b border-neutral-50 hover:bg-neutral-50 transition-colors"
                      variants={staggerItem}
                    >
                      <td className="px-4 py-3 font-mono text-xs text-neutral-800">
                        {row.clearanceNumber ?? '—'}
                      </td>
                      <td className="px-4 py-3 font-medium text-neutral-900">
                        {row.residentFullName}
                      </td>
                      <td className="px-4 py-3 text-neutral-600">
                        {PURPOSE_LABELS[row.purpose]}
                      </td>
                      <td className="px-4 py-3 text-neutral-600 capitalize">
                        {row.urgency.toLowerCase()}
                      </td>
                      <td className="px-4 py-3">
                        <Badge variant="status" value={row.status} dot />
                      </td>
                      <td className="px-4 py-3">
                        <Badge variant="payment" value={row.paymentStatus} dot />
                      </td>
                      <td className="px-4 py-3 text-neutral-500 text-xs">
                        {formatDate(row.issuedAt)}
                      </td>
                      <td className="px-4 py-3 text-neutral-500 text-xs">
                        {formatDate(row.createdAt)}
                      </td>
                    </motion.tr>
                  ))}
                </motion.tr>
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            ← Prev
          </Button>
          <span className="font-geist text-sm text-neutral-500">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
          >
            Next →
          </Button>
        </div>
      )}
    </div>
  );
}
