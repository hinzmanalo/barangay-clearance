'use client';

import type { ClearanceStatus } from '@/types/clearance';
import { STATUS_LABELS } from '@/types/clearance';

interface TimelineStep {
  status: ClearanceStatus;
  label: string;
}

const STEPS: TimelineStep[] = [
  { status: 'FOR_APPROVAL', label: 'For Approval' },
  { status: 'APPROVED', label: 'Approved' },
  { status: 'RELEASED', label: 'Released' },
];

const STATUS_ORDER: Record<ClearanceStatus, number> = {
  DRAFT: 0,
  FOR_APPROVAL: 1,
  APPROVED: 2,
  REJECTED: 1, // visual override — shown in red
  RELEASED: 3,
};

interface StatusTimelineProps {
  currentStatus: ClearanceStatus;
  reviewedAt?: string;
  issuedAt?: string;
}

/**
 * Visual step indicator showing the clearance request's progress through the
 * workflow. Rejected requests are shown with a red dot at the FOR_APPROVAL step.
 */
export default function StatusTimeline({ currentStatus, reviewedAt, issuedAt }: StatusTimelineProps) {
  const isRejected = currentStatus === 'REJECTED';
  const currentOrder = STATUS_ORDER[currentStatus];

  return (
    <div className="flex items-start gap-0">
      {STEPS.map((step, idx) => {
        const stepOrder = STATUS_ORDER[step.status];
        const isCompleted = !isRejected && currentOrder > stepOrder;
        const isActive = !isRejected && currentOrder === stepOrder;
        const isRejectedStep = isRejected && step.status === 'FOR_APPROVAL';

        return (
          <div key={step.status} className="flex items-center flex-1">
            {/* Connector line before step */}
            {idx > 0 && (
              <div
                className={`h-0.5 flex-1 ${
                  isCompleted || (currentOrder > stepOrder) ? 'bg-green-500' : 'bg-gray-200'
                }`}
              />
            )}

            {/* Step dot + label */}
            <div className="flex flex-col items-center">
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold border-2 ${
                  isRejectedStep
                    ? 'bg-red-100 border-red-500 text-red-700'
                    : isCompleted
                    ? 'bg-green-500 border-green-500 text-white'
                    : isActive
                    ? 'bg-blue-600 border-blue-600 text-white'
                    : 'bg-white border-gray-300 text-gray-400'
                }`}
              >
                {isCompleted ? '✓' : idx + 1}
              </div>
              <span
                className={`mt-1 text-xs font-medium ${
                  isRejectedStep
                    ? 'text-red-600'
                    : isCompleted
                    ? 'text-green-600'
                    : isActive
                    ? 'text-blue-600'
                    : 'text-gray-400'
                }`}
              >
                {isRejectedStep ? STATUS_LABELS['REJECTED'] : step.label}
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
