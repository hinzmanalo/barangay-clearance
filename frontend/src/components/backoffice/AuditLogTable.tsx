'use client';

import { Fragment, useState } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { TableRowSkeleton } from '@/components/shared/LoadingSkeleton';
import type { AuditLog } from '@/types/audit';

/** Colour-coding for common action prefixes */
const ACTION_STYLES: Record<string, string> = {
  USER_LOGIN_FAILED: 'bg-red-100 text-red-700',
  PAYMENT_FAILED:    'bg-red-100 text-red-700',
  PAYMENT_SUCCESS:   'bg-green-100 text-green-700',
  CLEARANCE_RELEASED:'bg-green-100 text-green-700',
  CLEARANCE_APPROVED:'bg-blue-100 text-blue-700',
  CLEARANCE_REJECTED:'bg-orange-100 text-orange-700',
};

function getActionStyle(action: string): string {
  return ACTION_STYLES[action] ?? 'bg-gray-100 text-gray-700';
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-PH', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

interface AuditLogTableProps {
  logs: AuditLog[];
  isLoading?: boolean;
}

/**
 * Paginated audit log table with expandable detail rows.
 *
 * Columns: Timestamp | Actor | Action | Entity Type | Entity ID | IP Address | (expand)
 */
export default function AuditLogTable({ logs, isLoading }: AuditLogTableProps) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const toggle = (id: string) =>
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });

  const HEADERS = ['Timestamp', 'Actor', 'Action', 'Entity Type', 'Entity ID', 'IP Address', ''];

  if (isLoading) {
    return (
      <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {HEADERS.map((h, i) => (
                <th key={i} className="px-4 py-3 text-left font-semibold text-gray-600">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {Array.from({ length: 8 }).map((_, i) => (
              <TableRowSkeleton key={i} cols={HEADERS.length} />
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (logs.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500 text-sm">No audit records found.</div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-gray-200 text-sm">
        <thead className="bg-gray-50">
          <tr>
            {HEADERS.map((h, i) => (
              <th
                key={i}
                className="px-4 py-3 text-left font-semibold text-gray-600 whitespace-nowrap"
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {logs.map((log) => {
            const isOpen = expanded.has(log.id);
            return (
              <Fragment key={log.id}>
                <tr
                  className="hover:bg-gray-50 transition-colors cursor-pointer"
                  onClick={() => log.details && toggle(log.id)}
                >
                  {/* Timestamp */}
                  <td className="px-4 py-3 whitespace-nowrap text-gray-600 text-xs">
                    {formatDate(log.createdAt)}
                  </td>

                  {/* Actor */}
                  <td className="px-4 py-3 text-gray-700 text-xs">
                    {log.actorEmail ?? (
                      <span className="italic text-gray-400">
                        {log.userId ? 'Deleted user' : 'Unauthenticated'}
                      </span>
                    )}
                  </td>

                  {/* Action badge */}
                  <td className="px-4 py-3">
                    <span
                      className={[
                        'inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold tracking-wide uppercase',
                        getActionStyle(log.action),
                      ].join(' ')}
                    >
                      {log.action}
                    </span>
                  </td>

                  {/* Entity Type */}
                  <td className="px-4 py-3 text-gray-600 text-xs">{log.entityType ?? '—'}</td>

                  {/* Entity ID */}
                  <td className="px-4 py-3 text-gray-500 text-xs font-mono truncate max-w-[120px]">
                    {log.entityId
                      ? <span title={log.entityId}>{log.entityId.split('-')[0]}&hellip;</span>
                      : '—'}
                  </td>

                  {/* IP Address */}
                  <td className="px-4 py-3 text-gray-500 text-xs font-mono">
                    {log.ipAddress ?? '—'}
                  </td>

                  {/* Expand toggle */}
                  <td className="px-4 py-3 text-gray-400">
                    {log.details && (
                      isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />
                    )}
                  </td>
                </tr>

                {/* Expanded detail row */}
                {isOpen && log.details && (
                  <tr key={`${log.id}-detail`} className="bg-gray-50">
                    <td colSpan={HEADERS.length} className="px-6 py-3">
                      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1">
                        Details
                      </p>
                      <pre className="text-xs text-gray-700 whitespace-pre-wrap break-words font-mono bg-white border border-gray-200 rounded p-3">
                        {log.details}
                      </pre>
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
