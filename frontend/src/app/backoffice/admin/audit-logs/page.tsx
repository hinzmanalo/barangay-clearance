'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import AuditLogTable from '@/components/backoffice/AuditLogTable';
import { useAuditLogs } from '@/hooks/useAuditLogs';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { staggerContainer, staggerItem } from '@/lib/animations';
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

  const hasActiveFilters = action || entityType || userIdInput || from || to;

  function resetFilters() {
    setAction('');
    setEntityType('');
    setUserIdInput('');
    setFrom('');
    setTo('');
    setPage(0);
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 space-y-8">
      {/* Page Header */}
      <PageHeader
        title="Audit Logs"
        description="System-wide record of all state-changing operations. Click a row to expand details."
      />

      {/* Filters */}
      <Card className="p-5">
        <div className="flex flex-wrap gap-4 items-end">
          <Select
            label="Action"
            value={action}
            onChange={(e) => setAction(e.target.value)}
          >
            <option value="">All actions</option>
            {ACTION_OPTIONS.filter(Boolean).map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
          </Select>

          <Select
            label="Entity Type"
            value={entityType}
            onChange={(e) => setEntityType(e.target.value)}
          >
            <option value="">All entity types</option>
            {ENTITY_TYPE_OPTIONS.filter(Boolean).map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </Select>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Actor User ID</label>
            <Input
              type="text"
              placeholder="UUID…"
              value={userIdInput}
              onChange={(e) => setUserIdInput(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">From</label>
            <Input
              type="datetime-local"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">To</label>
            <Input
              type="datetime-local"
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>

          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={resetFilters}>
              Clear Filters
            </Button>
          )}
        </div>
      </Card>

      {/* Table */}
      <AuditLogTable logs={logs} isLoading={isLoading} />

      {/* Pagination */}
      {totalPages > 1 && (
        <motion.div
          className="flex items-center justify-center gap-2"
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
        >
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
        </motion.div>
      )}
    </div>
  );
}
