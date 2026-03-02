import type { ClearancePaymentStatus } from '@/types/clearance';
import { PAYMENT_STATUS_LABELS } from '@/types/clearance';

const PAYMENT_STYLES: Record<ClearancePaymentStatus, string> = {
  UNPAID: 'bg-orange-100 text-orange-800 border-orange-200',
  PAID: 'bg-green-100 text-green-800 border-green-200',
  WAIVED: 'bg-gray-100 text-gray-600 border-gray-200',
};

interface PaymentBadgeProps {
  status: ClearancePaymentStatus;
  className?: string;
}

/**
 * Color-coded badge displaying a clearance request's payment status.
 */
export default function PaymentBadge({ status, className = '' }: PaymentBadgeProps) {
  const styles = PAYMENT_STYLES[status] ?? 'bg-gray-100 text-gray-700 border-gray-200';
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${styles} ${className}`}
    >
      {PAYMENT_STATUS_LABELS[status] ?? status}
    </span>
  );
}
