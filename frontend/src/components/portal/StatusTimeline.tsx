'use client';

import type { ClearanceStatus, ClearancePaymentStatus } from '@/types/clearance';
import { STATUS_LABELS, PAYMENT_STATUS_LABELS } from '@/types/clearance';

type StepState = 'completed' | 'active' | 'inactive' | 'rejected' | 'payment-pending' | 'payment-done';

interface StatusTimelineProps {
  currentStatus: ClearanceStatus;
  paymentStatus: ClearancePaymentStatus;
  reviewedAt?: string;
  issuedAt?: string;
}

/**
 * Visual step indicator showing the clearance request's progress through the
 * workflow, including the payment step between Approved and Released.
 * Rejected requests are shown with a red dot at the For Approval step.
 */
export default function StatusTimeline({
  currentStatus,
  paymentStatus,
  reviewedAt,
  issuedAt,
}: StatusTimelineProps) {
  const isRejected = currentStatus === 'REJECTED';
  const isPaymentDone = paymentStatus === 'PAID' || paymentStatus === 'WAIVED';

  /** Derive the visual state for each of the 4 steps (0-indexed). */
  function getStepState(stepIndex: number): StepState {
    if (isRejected) {
      return stepIndex === 0 ? 'rejected' : 'inactive';
    }

    switch (stepIndex) {
      case 0: // For Approval
        if (currentStatus === 'APPROVED' || currentStatus === 'RELEASED') return 'completed';
        if (currentStatus === 'FOR_APPROVAL') return 'active';
        return 'inactive'; // DRAFT

      case 1: // Approved
        if (currentStatus === 'APPROVED' || currentStatus === 'RELEASED') return 'completed';
        return 'inactive';

      case 2: // Payment
        if (currentStatus === 'RELEASED') return 'payment-done';
        if (currentStatus === 'APPROVED') {
          return isPaymentDone ? 'payment-done' : 'payment-pending';
        }
        return 'inactive';

      case 3: // Released
        if (currentStatus === 'RELEASED') return 'completed';
        return 'inactive';

      default:
        return 'inactive';
    }
  }

  /** Whether the connector segment between step (idx-1) and step (idx) is filled. */
  function isConnectorFilled(idx: number): boolean {
    if (isRejected) return false;
    // Connector is filled when the step to the LEFT of it is completed/done.
    const leftState = getStepState(idx - 1);
    return leftState === 'completed' || leftState === 'payment-done';
  }

  const STEPS = [
    { label: 'For Approval' },
    { label: 'Approved' },
    {
      label:
        currentStatus === 'APPROVED' && !isPaymentDone
          ? PAYMENT_STATUS_LABELS['UNPAID']
          : paymentStatus === 'WAIVED'
          ? PAYMENT_STATUS_LABELS['WAIVED']
          : PAYMENT_STATUS_LABELS['PAID'],
    },
    { label: 'Released' },
  ];

  return (
    <div className="flex items-start gap-0">
      {STEPS.map((step, idx) => {
        const state = getStepState(idx);
        const isPaymentStep = idx === 2;

        const dotClass =
          state === 'rejected'
            ? 'bg-red-100 border-red-500 text-red-700'
            : state === 'completed'
            ? 'bg-green-500 border-green-500 text-white'
            : state === 'payment-done'
            ? 'bg-green-500 border-green-500 text-white'
            : state === 'payment-pending'
            ? 'bg-orange-100 border-orange-500 text-orange-700'
            : state === 'active'
            ? 'bg-blue-600 border-blue-600 text-white'
            : 'bg-white border-gray-300 text-gray-400';

        const labelClass =
          state === 'rejected'
            ? 'text-red-600'
            : state === 'completed' || state === 'payment-done'
            ? 'text-green-600'
            : state === 'payment-pending'
            ? 'text-orange-600'
            : state === 'active'
            ? 'text-blue-600'
            : 'text-gray-400';

        const displayLabel =
          state === 'rejected' ? STATUS_LABELS['REJECTED'] : step.label;

        const dotContent =
          state === 'completed' || state === 'payment-done' ? '✓' : idx + 1;

        return (
          <div key={idx} className="flex items-center flex-1">
            {/* Connector line before step */}
            {idx > 0 && (
              <div
                className={`h-0.5 flex-1 ${
                  isConnectorFilled(idx) ? 'bg-green-500' : 'bg-gray-200'
                }`}
              />
            )}

            {/* Step dot + label */}
            <div className="flex flex-col items-center">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold border-2 ${dotClass}`}
              >
                {dotContent}
              </div>
              <span className={`mt-1 text-xs font-medium text-center ${labelClass}`}>
                {displayLabel}
              </span>
            </div>

            {/* Connector line after last step */}
            {idx === STEPS.length - 1 && (
              <div className="h-0.5 flex-1 bg-gray-200" />
            )}
          </div>
        );
      })}
    </div>
  );
}
