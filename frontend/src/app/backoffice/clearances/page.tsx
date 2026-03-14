'use client';

import { useState } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import ClearanceTable from '@/components/backoffice/ClearanceTable';
import { useClearances, useClearanceSummary } from '@/hooks/useClearances';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { staggerContainer, staggerItem } from '@/lib/animations';
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

  const { data: summary, isLoading: summaryLoading } = useClearanceSummary();
  const clearances = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const handleClearFilters = () => {
    setStatus('');
    setPaymentStatus('');
    setPage(0);
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Clearance Requests"
        description="Manage and approve resident clearance requests"
        actions={
          <Link href="/backoffice/clearances/new">
            <Button variant="primary" size="sm">
              + Walk-in Request
            </Button>
          </Link>
        }
      />

      {/* Summary strip */}
      {summaryLoading ? (
        <div className="flex gap-8 px-6 py-4 bg-white rounded-lg border border-neutral-200">
          {[1, 2, 3, 4].map((i) => (
            <div key={i}>
              <Skeleton className="h-3 w-20 mb-2" />
              <Skeleton className="h-6 w-12" />
            </div>
          ))}
        </div>
      ) : summary ? (
        <motion.div
          className="flex gap-8 px-6 py-4 bg-white rounded-lg border border-neutral-200"
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
        >
          <motion.div variants={staggerItem}>
            <span className="font-geist text-xs text-neutral-500 uppercase tracking-wide">Pending</span>
            <p className="font-sora font-bold text-xl text-amber-600 mt-1">{summary.pendingApproval}</p>
          </motion.div>
          <motion.div variants={staggerItem}>
            <span className="font-geist text-xs text-neutral-500 uppercase tracking-wide">Approved</span>
            <p className="font-sora font-bold text-xl text-teal-600 mt-1">{summary.approved}</p>
          </motion.div>
          <motion.div variants={staggerItem}>
            <span className="font-geist text-xs text-neutral-500 uppercase tracking-wide">Awaiting Payment</span>
            <p className="font-sora font-bold text-xl text-orange-600 mt-1">{summary.awaitingPayment}</p>
          </motion.div>
          <motion.div variants={staggerItem}>
            <span className="font-geist text-xs text-neutral-500 uppercase tracking-wide">Released</span>
            <p className="font-sora font-bold text-xl text-blue-600 mt-1">{summary.releasedToday}</p>
          </motion.div>
        </motion.div>
      ) : null}

      {/* Filter bar */}
      <Card className="p-4">
        <div className="flex flex-wrap gap-3 items-end">
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

          <Button
            variant="primary"
            size="sm"
            onClick={() => {
              /* Search trigger if needed */
            }}
          >
            Search
          </Button>

          {(status || paymentStatus) && (
            <Button variant="ghost" size="sm" onClick={handleClearFilters}>
              Clear
            </Button>
          )}
        </div>
      </Card>

      {/* Table */}
      <ClearanceTable clearances={clearances} isLoading={isLoading} />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            ← Prev
          </Button>
          <span className="font-geist text-sm text-neutral-500">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
          >
            Next →
          </Button>
        </div>
      )}
    </div>
  );
}
