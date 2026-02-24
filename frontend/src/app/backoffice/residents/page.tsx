'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import ResidentTable from '@/components/backoffice/ResidentTable';
import { useResidents, usePendingResidents, useActivateResident, useRejectResident } from '@/hooks/useResidents';
import type { Resident } from '@/types/resident';
import { AxiosError } from 'axios';

export default function ResidentsPage() {
  const [searchInput, setSearchInput] = useState('');
  const [purokInput, setPurokInput] = useState('');
  const [debouncedQ, setDebouncedQ] = useState('');
  const [debouncedPurok, setDebouncedPurok] = useState('');
  const [page, setPage] = useState(0);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

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
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
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
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-8">
      {/* Toast */}
      {toast && (
        <div
          className={`fixed top-4 right-4 z-50 rounded-lg px-4 py-3 text-sm text-white shadow-lg ${
            toast.type === 'success' ? 'bg-green-600' : 'bg-red-600'
          }`}
        >
          {toast.message}
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Residents</h1>
          <p className="mt-1 text-sm text-gray-500">Manage the barangay resident registry.</p>
        </div>
        <Link
          href="/backoffice/residents/new"
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + Add Resident
        </Link>
      </div>

      {/* Pending registrations */}
      {pendingResidents && pendingResidents.length > 0 && (
        <section className="rounded-lg border border-yellow-200 bg-yellow-50 p-4 space-y-3">
          <h2 className="font-semibold text-yellow-800 text-sm">
            Pending Portal Registrations ({pendingResidents.length})
          </h2>
          <div className="space-y-2">
            {pendingResidents.map((r) => (
              <div key={r.id} className="flex items-center justify-between bg-white rounded-md border border-yellow-100 px-4 py-2">
                <span className="text-sm text-gray-800 font-medium">
                  {r.lastName}, {r.firstName} {r.middleName ?? ''}
                  {r.email && <span className="ml-2 text-gray-500 font-normal">({r.email})</span>}
                </span>
                <div className="flex gap-2">
                  <button
                    onClick={() => r.userId && handleActivate(r.userId)}
                    disabled={activateMutation.isPending}
                    className="rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
                  >
                    Activate
                  </button>
                  <button
                    onClick={() => r.userId && handleReject(r.userId)}
                    disabled={rejectMutation.isPending}
                    className="rounded-md bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                  >
                    Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Search filters */}
      <div className="flex gap-4">
        <input
          type="text"
          placeholder="Search by name…"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <input
          type="text"
          placeholder="Filter by purok/zone…"
          value={purokInput}
          onChange={(e) => setPurokInput(e.target.value)}
          className="w-56 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {/* Resident table */}
      <ResidentTable residents={residents} isLoading={isLoading} />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-600">
          <span>
            Page {page + 1} of {totalPages} ({data?.totalElements ?? 0} total)
          </span>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="rounded-md border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:opacity-40"
            >
              Previous
            </button>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="rounded-md border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
