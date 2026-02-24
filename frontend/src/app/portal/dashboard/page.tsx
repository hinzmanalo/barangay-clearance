'use client';

import { useState } from 'react';
import Link from 'next/link';
import RequestCard from '@/components/portal/RequestCard';
import { useMyClearances } from '@/hooks/useClearances';

export default function PortalDashboardPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useMyClearances({ page, size: 10 });

  const requests = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">My Clearance Requests</h1>
        <Link
          href="/portal/requests/new"
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + New Request
        </Link>
      </div>

      {isLoading ? (
        <div className="text-sm text-gray-500 py-8 text-center">Loading…</div>
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-lg border border-gray-200 p-8 text-center">
          <p className="text-gray-500 text-sm mb-4">You haven't submitted any clearance requests yet.</p>
          <Link
            href="/portal/requests/new"
            className="inline-flex rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            Submit your first request
          </Link>
        </div>
      ) : (
        <div className="space-y-3">
          {requests.map((req) => (
            <RequestCard key={req.id} request={req} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-2">
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
