'use client';

import Link from 'next/link';
import type { Resident } from '@/types/resident';

interface ResidentTableProps {
  residents: Resident[];
  isLoading?: boolean;
}

export default function ResidentTable({ residents, isLoading }: ResidentTableProps) {
  if (isLoading) {
    return (
      <div className="text-center py-12 text-gray-500 text-sm">Loading residents…</div>
    );
  }

  if (residents.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500 text-sm">No residents found.</div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Name</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Address</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Contact</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Status</th>
            <th className="px-4 py-3 text-left font-semibold text-gray-600">Portal</th>
            <th className="px-4 py-3" />
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {residents.map((r) => (
            <tr key={r.id} className="hover:bg-gray-50 transition-colors">
              <td className="px-4 py-3 font-medium text-gray-900">
                {r.lastName}, {r.firstName}
                {r.middleName ? ` ${r.middleName.charAt(0)}.` : ''}
              </td>
              <td className="px-4 py-3 text-gray-600 max-w-xs truncate">{r.address}</td>
              <td className="px-4 py-3 text-gray-600">{r.contactNumber ?? '—'}</td>
              <td className="px-4 py-3">
                <span
                  className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
                    r.status === 'ACTIVE'
                      ? 'bg-green-100 text-green-700'
                      : 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {r.status}
                </span>
              </td>
              <td className="px-4 py-3">
                {r.hasPortalAccount ? (
                  <span className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium bg-blue-100 text-blue-700">
                    Registered
                  </span>
                ) : (
                  <span className="text-gray-400 text-xs">Walk-in</span>
                )}
              </td>
              <td className="px-4 py-3 text-right">
                <Link
                  href={`/backoffice/residents/${r.id}`}
                  className="text-blue-600 hover:underline text-xs font-medium"
                >
                  View
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
