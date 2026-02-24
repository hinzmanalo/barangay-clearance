'use client';

import Link from 'next/link';

export default function BackofficeDashboardPage() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <h2 className="text-xl font-bold text-gray-900 mb-6">Dashboard</h2>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <DashboardCard
          title="Residents"
          description="Manage resident registrations and portal activation."
          href="/backoffice/residents"
        />
        <DashboardCard
          title="Clearances"
          description="Review, approve, and release clearance requests."
          href="/backoffice/clearances"
        />
        <DashboardCard
          title="Reports"
          description="View clearance issuance reports and statistics."
          href="/backoffice/reports"
        />
      </div>

      <p className="mt-8 text-xs text-gray-400">
        Barangay Clearance System — Phase development build
      </p>
    </div>
  );
}

function DashboardCard({
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
