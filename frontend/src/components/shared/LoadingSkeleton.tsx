interface SkeletonProps {
  className?: string;
}

/** Animated rectangle placeholder used while content is loading. */
export function Skeleton({ className = '' }: SkeletonProps) {
  return (
    <div
      className={`animate-pulse rounded bg-gray-200 ${className}`}
      aria-hidden="true"
    />
  );
}

/** Table-row skeleton — mimics a row with several columns. */
export function TableRowSkeleton({ cols = 5 }: { cols?: number }) {
  return (
    <tr>
      {Array.from({ length: cols }).map((_, i) => (
        <td key={i} className="px-4 py-3">
          <Skeleton className="h-4 w-full" />
        </td>
      ))}
    </tr>
  );
}

/** Card skeleton — mimics a stat/summary card. */
export function CardSkeleton() {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 space-y-2">
      <Skeleton className="h-3 w-24" />
      <Skeleton className="h-8 w-16" />
    </div>
  );
}

/** Full-page loading skeleton for list pages. */
export function ListPageSkeleton({ rows = 5, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-9 w-32 rounded-md" />
      </div>

      {/* Table */}
      <div className="rounded-lg border border-gray-200 bg-white overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              {Array.from({ length: cols }).map((_, i) => (
                <th key={i} className="px-4 py-3">
                  <Skeleton className="h-3 w-full" />
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {Array.from({ length: rows }).map((_, i) => (
              <TableRowSkeleton key={i} cols={cols} />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/** Detail page skeleton. */
export function DetailPageSkeleton() {
  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      <Skeleton className="h-7 w-48" />
      <div className="rounded-lg border border-gray-200 bg-white p-6 space-y-4">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="space-y-1">
            <Skeleton className="h-3 w-24" />
            <Skeleton className="h-5 w-2/3" />
          </div>
        ))}
      </div>
    </div>
  );
}

export default Skeleton;
