'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Users } from 'lucide-react';
import Link from 'next/link';
import UserTable from '@/components/backoffice/UserTable';
import { useUsers } from '@/hooks/useUsers';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { staggerContainer, staggerItem } from '@/lib/animations';
import type { StaffUser } from '@/types/auth';

const ROLE_OPTIONS = ['', 'ADMIN', 'CLERK', 'APPROVER'] as const;
const STATUS_OPTIONS = ['', 'ACTIVE', 'INACTIVE', 'DEACTIVATED'] as const;

export default function UsersPage() {
  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  // Debounce search input by 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchInput);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // Reset page when filters change
  useEffect(() => {
    setPage(0);
  }, [role, status]);

  const { data, isLoading } = useUsers({
    search: debouncedSearch || undefined,
    role: role || undefined,
    status: status || undefined,
    page,
    size: 20,
  });

  const users: StaffUser[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const hasActiveFilters = searchInput || role || status;

  function resetFilters() {
    setSearchInput('');
    setRole('');
    setStatus('');
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 space-y-8">
      {/* Page Header */}
      <PageHeader
        title="User Accounts"
        description="Manage staff accounts for the back-office system."
        actions={
          <Link href="/backoffice/admin/users/new">
            <Button variant="primary">+ New User</Button>
          </Link>
        }
      />

      {/* Filters */}
      <Card className="p-5">
        <div className="flex flex-wrap gap-4 items-end">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">Search</label>
            <Input
              type="text"
              placeholder="By name or email…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
          </div>

          <Select
            label="Role"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All roles</option>
            {ROLE_OPTIONS.filter(Boolean).map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </Select>

          <Select
            label="Status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            {STATUS_OPTIONS.filter(Boolean).map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </Select>

          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={resetFilters}>
              Clear Filters
            </Button>
          )}
        </div>
      </Card>

      {/* Table */}
      <UserTable users={users} isLoading={isLoading} />

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
