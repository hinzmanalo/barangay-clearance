'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { UserCheck } from 'lucide-react';
import ResidentTable from '@/components/backoffice/ResidentTable';
import { useResidents, usePendingResidents, useActivateResident, useRejectResident } from '@/hooks/useResidents';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import type { Resident } from '@/types/resident';
import { AxiosError } from 'axios';
import { toast } from '@/components/shared/ErrorToast';

export default function ResidentsPage() {
  const [searchInput, setSearchInput] = useState('');
  const [purokInput, setPurokInput] = useState('');
  const [debouncedQ, setDebouncedQ] = useState('');
  const [debouncedPurok, setDebouncedPurok] = useState('');
  const [page, setPage] = useState(0);

  // Debounce the search inputs by 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQ(searchInput);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedPurok(purokInput);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [purokInput]);

  const { data, isLoading } = useResidents({ q: debouncedQ, purok: debouncedPurok, page, size: 20 });
  const { data: pendingResidents, isLoading: pendingLoading } = usePendingResidents();
  const activateMutation = useActivateResident();
  const rejectMutation = useRejectResident();

  const showToast = useCallback((message: string, type: 'success' | 'error') => {
    if (type === 'success') toast.success(message);
    else toast.error(message);
  }, []);

  const handleActivate = async (userId: string) => {
    try {
      await activateMutation.mutateAsync(userId);
      showToast('Account activated successfully.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      showToast(axiosErr.response?.data?.message ?? 'Failed to activate account.', 'error');
    }
  };

  const handleReject = async (userId: string) => {
    try {
      await rejectMutation.mutateAsync(userId);
      showToast('Account rejected.', 'success');
    } catch (err) {
      const axiosErr = err as AxiosError<{ message: string }>;
      showToast(axiosErr.response?.data?.message ?? 'Failed to reject account.', 'error');
    }
  };

  const residents: Resident[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 space-y-8">
      {/* Page Header */}
      <PageHeader
        title="Residents"
        description="Manage the barangay resident registry"
        actions={
          <Link href="/backoffice/residents/new">
            <Button variant="primary" size="sm">
              + Add Resident
            </Button>
          </Link>
        }
      />

      {/* Pending portal activations */}
      {pendingResidents && pendingResidents.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, ease: 'easeOut' }}
        >
          <Card accentColor="amber" className="p-5">
            <div className="flex items-center gap-2 mb-4">
              <UserCheck className="w-5 h-5 text-amber-600" />
              <h3 className="font-sora font-semibold text-base text-neutral-900">
                Pending Portal Activations ({pendingResidents.length})
              </h3>
            </div>
            <div className="space-y-2">
              {pendingResidents.map((r) => (
                <div key={r.id} className="flex items-center justify-between bg-neutral-50 rounded-lg border border-amber-100 px-4 py-3">
                  <div className="flex-1">
                    <p className="font-geist text-sm font-medium text-neutral-900">
                      {r.lastName}, {r.firstName} {r.middleName ?? ''}
                    </p>
                    {r.email && (
                      <p className="font-geist text-xs text-neutral-500 mt-0.5">{r.email}</p>
                    )}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => r.userId && handleActivate(r.userId)}
                      loading={activateMutation.isPending}
                      disabled={activateMutation.isPending}
                    >
                      Activate
                    </Button>
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => r.userId && handleReject(r.userId)}
                      loading={rejectMutation.isPending}
                      disabled={rejectMutation.isPending}
                    >
                      Reject
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </motion.div>
      )}

      {/* Search filters */}
      <div className="flex flex-col sm:flex-row gap-4">
        <Input
          type="text"
          placeholder="Search by name…"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="flex-1"
        />
        <Input
          type="text"
          placeholder="Filter by purok/zone…"
          value={purokInput}
          onChange={(e) => setPurokInput(e.target.value)}
          className="sm:w-56"
        />
      </div>

      {/* Resident table */}
      <ResidentTable residents={residents} isLoading={isLoading} />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
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
            onClick={() => setPage((p) => p + 1)}
          >
            Next →
          </Button>
        </div>
      )}
    </div>
  );
}
