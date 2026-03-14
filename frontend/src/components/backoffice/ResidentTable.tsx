'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import type { Resident } from '@/types/resident';
import { Badge } from '@/components/ui/Badge';
import { Skeleton } from '@/components/ui/Skeleton';
import { staggerContainer, staggerItem } from '@/lib/animations';

interface ResidentTableProps {
  residents: Resident[];
  isLoading?: boolean;
}

export default function ResidentTable({ residents, isLoading }: ResidentTableProps) {
  if (isLoading) {
    return (
      <div className="overflow-x-auto rounded-lg border border-neutral-200 bg-white">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-neutral-100 bg-neutral-50">
              {['Name', 'Address', 'Contact', 'Status', 'Portal', ''].map((h, i) => (
                <th
                  key={i}
                  className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: 5 }).map((_, i) => (
              <tr key={i} className="border-b border-neutral-50">
                {Array.from({ length: 6 }).map((_, j) => (
                  <td key={j} className="px-4 py-3">
                    <Skeleton className="h-4 w-20" />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (residents.length === 0) {
    return (
      <div className="text-center py-12 text-neutral-500 text-sm">No residents found.</div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-neutral-200 bg-white">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-neutral-100 bg-neutral-50">
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Name
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Address
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Contact
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Status
            </th>
            <th className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium">
              Portal
            </th>
            <th className="px-4 py-3" />
          </tr>
        </thead>
        <tbody>
          <motion.tr
            className="contents"
            variants={staggerContainer}
            initial="hidden"
            animate="visible"
          >
            {residents.map((r) => (
              <motion.tr
                key={r.id}
                className="border-b border-neutral-50 hover:bg-neutral-50 transition-colors"
                variants={staggerItem}
              >
                <td className="px-4 py-3 font-medium text-neutral-900">
                  {r.lastName}, {r.firstName}
                  {r.middleName ? ` ${r.middleName.charAt(0)}.` : ''}
                </td>
                <td className="px-4 py-3 text-neutral-600 max-w-xs truncate">{r.address}</td>
                <td className="px-4 py-3 text-neutral-600">{r.contactNumber ?? '—'}</td>
                <td className="px-4 py-3">
                  <Badge variant="user-status" value={r.status} dot />
                </td>
                <td className="px-4 py-3">
                  {r.hasPortalAccount && r.portalStatus ? (
                    <Badge variant="user-status" value={r.portalStatus} dot />
                  ) : (
                    <span className="text-neutral-400 text-xs">No account</span>
                  )}
                </td>
                <td className="px-4 py-3 text-right">
                  <Link
                    href={`/backoffice/residents/${r.id}`}
                    className="text-blue-600 hover:text-blue-700 transition-colors text-xs font-medium"
                  >
                    View
                  </Link>
                </td>
              </motion.tr>
            ))}
          </motion.tr>
        </tbody>
      </table>
    </div>
  );
}
