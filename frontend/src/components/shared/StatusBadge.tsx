import type { ClearanceStatus } from '@/types/clearance';
import { STATUS_LABELS } from '@/types/clearance';

const STATUS_STYLES: Record<ClearanceStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-700 border-gray-200',
  FOR_APPROVAL: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  APPROVED: 'bg-green-100 text-green-800 border-green-200',
  REJECTED: 'bg-red-100 text-red-700 border-red-200',
  RELEASED: 'bg-blue-100 text-blue-800 border-blue-200',
};

interface StatusBadgeProps {
  status: ClearanceStatus;
  className?: string;
}

/**
 * Color-coded badge displaying a clearance request's current status.
 */
export default function StatusBadge({ status, className = '' }: StatusBadgeProps) {
  const styles = STATUS_STYLES[status] ?? 'bg-gray-100 text-gray-700 border-gray-200';
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${styles} ${className}`}
    >
      {STATUS_LABELS[status] ?? status}
    </span>
  );
}
