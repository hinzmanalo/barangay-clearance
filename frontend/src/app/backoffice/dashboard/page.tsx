'use client';

import Link from 'next/link';
import { useClearanceSummary } from '@/hooks/useClearances';
import { CardSkeleton } from '@/components/shared/LoadingSkeleton';

interface StatCardProps {
  title: string;
  value: number;
  href: string;
  colorClass: string;
  titleColor: string;
  valueColor: string;
}

function StatCard({ title, value, href, colorClass, titleColor, valueColor }: StatCardProps) {
  return (
    <Link
      href={href}
      className={`block rounded-lg border p-4 bg-white hover:shadow-md transition ${colorClass}`}
    >
      <p className={`text-xs font-medium uppercase tracking-wide ${titleColor}`}>{title}</p>
      <p className={`mt-1 text-3xl font-bold ${valueColor}`}>{value}</p>
    </Link>
  );
}

export default function BackofficeDashboardPage() {
  const { data: summary, isLoading } = useClearanceSummary({ refetchInterval: 30_000 });

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 space-y-6">
      <h2 className="text-xl font-bold text-gray-900">Dashboard</h2>

      {/* Summary stat cards with auto-refresh */}
      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <CardSkeleton />
          <CardSkeleton />
          <CardSkeleton />
        </div>
      ) : summary ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <StatCard
            title="Pending Review"
            value={summary.pendingApproval}
            href="/backoffice/clearances?status=FOR_APPROVAL"
            colorClass="border-yellow-200"
            titleColor="text-yellow-600"
            valueColor="text-yellow-700"
          />
          <StatCard
            title="Approved — Awaiting Payment"
            value={summary.awaitingPayment}
            href="/backoffice/clearances?status=APPROVED"
            colorClass="border-orange-200"
            titleColor="text-orange-600"
            valueColor="text-orange-700"
          />
          <StatCard
            title="Released Today"
            value={summary.releasedToday}
            href="/backoffice/clearances?status=RELEASED"
            colorClass="border-blue-200"
            titleColor="text-blue-600"
            valueColor="text-blue-700"
          />
        </div>
      ) : null}

      {/* Quick navigation cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <NavCard
          title="Residents"
          description="Manage resident registrations and portal activation."
          href="/backoffice/residents"
        />
        <NavCard
          title="Clearances"
          description="Review, approve, and release clearance requests."
          href="/backoffice/clearances"
        />
        <NavCard
          title="Reports"
          description="View clearance issuance reports and statistics."
          href="/backoffice/reports"
        />
      </div>

      <p className="text-xs text-gray-400">
        Stats refresh automatically every 30 seconds.
      </p>
    </div>
  );
}

function NavCard({
  title,
  description,
  href,
}: {
  title: string;
  description: string;
  href: string;
}) {
  return (
    <Link
      href={href}
      className="block rounded-lg border border-gray-200 bg-white p-5 shadow-sm hover:shadow-md hover:border-blue-300 transition"
    >
      <h3 className="font-semibold text-gray-800">{title}</h3>
      <p className="mt-1 text-sm text-gray-500">{description}</p>
    </Link>
  );
}
