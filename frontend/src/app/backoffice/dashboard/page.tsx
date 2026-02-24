'use client';

import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';

export default function BackofficeDashboardPage() {
  const { email, role, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top nav */}
      <header className="bg-white shadow-sm">
        <div className="mx-auto max-w-7xl px-4 py-4 flex items-center justify-between">
          <h1 className="text-lg font-semibold text-gray-900">Barangay Clearance — Back Office</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">
              {email}{' '}
              <span className="ml-1 rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                {role}
              </span>
            </span>
            <button
              onClick={logout}
              className="text-sm text-red-600 hover:text-red-800 font-medium"
            >
              Log out
            </button>
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto max-w-7xl px-4 py-8">
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
      </main>
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
