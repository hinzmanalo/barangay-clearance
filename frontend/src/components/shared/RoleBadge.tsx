import type { Role } from '@/types/auth';

const ROLE_STYLES: Record<Role, string> = {
  ADMIN: 'bg-red-100 text-red-800 border-red-200',
  APPROVER: 'bg-purple-100 text-purple-800 border-purple-200',
  CLERK: 'bg-blue-100 text-blue-800 border-blue-200',
  RESIDENT: 'bg-gray-100 text-gray-700 border-gray-200',
};

const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Admin',
  APPROVER: 'Approver',
  CLERK: 'Clerk',
  RESIDENT: 'Resident',
};

interface RoleBadgeProps {
  role: Role;
  className?: string;
}

/**
 * Color-coded chip showing a user's role.
 */
export default function RoleBadge({ role, className = '' }: RoleBadgeProps) {
  const styles = ROLE_STYLES[role] ?? 'bg-gray-100 text-gray-700 border-gray-200';
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${styles} ${className}`}
    >
      {ROLE_LABELS[role] ?? role}
    </span>
  );
}
