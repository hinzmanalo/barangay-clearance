'use client';

import { useState } from 'react';
import type { ClearanceRequest, ClearanceStatus } from '@/types/clearance';
import { useApproveClearance, useRejectClearance, useReleaseClearance, useMarkClearancePaid } from '@/hooks/useClearances';
import { AxiosError } from 'axios';

interface ActionButtonsProps {
  clearance: ClearanceRequest;
  onSuccess?: (updated: ClearanceRequest) => void;
}

/**
 * Staff action buttons for a clearance request.
 * Renders only the valid actions based on current status.
 */
export default function ActionButtons({ clearance, onSuccess }: ActionButtonsProps) {
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const approveMutation = useApproveClearance();
  const rejectMutation = useRejectClearance();
  const releaseMutation = useReleaseClearance();
  const markPaidMutation = useMarkClearancePaid();

  const handleApprove = async () => {
    setError(null);
    try {
      const updated = await approveMutation.mutateAsync(clearance.id);
      onSuccess?.(updated);
    } catch (err) {
      const e = err as AxiosError<{ message: string }>;
      setError(e.response?.data?.message ?? 'Failed to approve request.');
    }
  };

  const handleReject = async () => {
    if (!rejectReason.trim()) {
      setError('Rejection reason is required.');
      return;
    }
    setError(null);
    try {
      const updated = await rejectMutation.mutateAsync({ id: clearance.id, payload: { reason: rejectReason } });
      setShowRejectForm(false);
      setRejectReason('');
      onSuccess?.(updated);
    } catch (err) {
      const e = err as AxiosError<{ message: string }>;
      setError(e.response?.data?.message ?? 'Failed to reject request.');
    }
  };

  const handleRelease = async () => {
    setError(null);
    try {
      const updated = await releaseMutation.mutateAsync(clearance.id);
      onSuccess?.(updated);
    } catch (err) {
      const e = err as AxiosError<{ message: string }>;
      setError(e.response?.data?.message ?? 'Failed to release request.');
    }
  };

  const handleMarkPaid = async () => {
    setError(null);
    try {
      await markPaidMutation.mutateAsync(clearance.id);
      // Re-fetch is handled by cache invalidation in the hook;
      // trigger an optional success callback if the parent wants to refresh
      onSuccess?.(clearance);
    } catch (err) {
      const e = err as AxiosError<{ message: string }>;
      setError(e.response?.data?.message ?? 'Failed to mark as paid.');
    }
  };

  const { status, paymentStatus } = clearance;
  const canApprove = status === 'FOR_APPROVAL';
  const canReject = status === 'FOR_APPROVAL';
  const canMarkPaid = status === 'APPROVED' && paymentStatus === 'UNPAID';
  const canRelease = status === 'APPROVED' && paymentStatus === 'PAID';
  const hasActions = canApprove || canReject || canMarkPaid || canRelease;

  if (!hasActions) return null;

  const isPending =
    approveMutation.isPending || rejectMutation.isPending ||
    releaseMutation.isPending || markPaidMutation.isPending;

  return (
    <div className="space-y-3">
      {error && (
        <div className="rounded-md bg-red-50 border border-red-200 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        {canApprove && (
          <button
            onClick={handleApprove}
            disabled={isPending}
            className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
          >
            {approveMutation.isPending ? 'Approving…' : 'Approve'}
          </button>
        )}

        {canReject && !showRejectForm && (
          <button
            onClick={() => { setShowRejectForm(true); setError(null); }}
            disabled={isPending}
            className="rounded-md border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:opacity-50"
          >
            Reject
          </button>
        )}

        {canRelease && (
          <button
            onClick={handleRelease}
            disabled={isPending}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {releaseMutation.isPending ? 'Releasing…' : 'Release'}
          </button>
        )}

        {canMarkPaid && (
          <button
            onClick={handleMarkPaid}
            disabled={isPending}
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
          >
            {markPaidMutation.isPending ? 'Recording…' : 'Mark as Paid (Cash)'}
          </button>
        )}
      </div>

      {/* Inline reject form */}
      {showRejectForm && (
        <div className="rounded-md border border-red-200 bg-red-50 p-4 space-y-3">
          <p className="text-sm font-medium text-red-800">Reason for rejection *</p>
          <textarea
            rows={3}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="Describe why the request is being rejected…"
            className="w-full rounded-md border border-red-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-400"
          />
          <div className="flex gap-2">
            <button
              onClick={handleReject}
              disabled={isPending}
              className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
            >
              {rejectMutation.isPending ? 'Rejecting…' : 'Confirm Reject'}
            </button>
            <button
              onClick={() => { setShowRejectForm(false); setRejectReason(''); setError(null); }}
              disabled={isPending}
              className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
