"use client";

import React from "react";
import { motion } from "framer-motion";
import { staggerContainer, staggerItem } from "@/lib/animations";
import { TableRowSkeleton } from "./Skeleton";
import clsx from "clsx";

export interface DataTableColumn<T> {
  key: keyof T;
  label: string;
  render?: (row: T) => React.ReactNode;
}

export interface DataTableProps<T> {
  columns: DataTableColumn<T>[];
  header?: React.ReactNode;
  loading?: boolean;
  skeletonRows?: number;
}

/**
 * DataTable wrapper component.
 */
export const DataTable = <T,>({
  columns,
  header,
  loading = false,
  skeletonRows = 5,
  children,
}: Readonly<DataTableProps<T> & { children?: React.ReactNode }>) => {
  return (
    <div className="bg-white rounded-lg overflow-hidden shadow-sm">
      {header && <div className="border-b border-neutral-100 p-4">{header}</div>}

      <div className="overflow-x-auto">
        <table className="w-full">
          <TableHead columns={columns} />

          {loading ? (
            <tbody>
              {Array.from({ length: skeletonRows }).map((_, i) => (
                <TableRowSkeleton key={i} columns={columns.length} />
              ))}
            </tbody>
          ) : (
            <tbody>
              <motion.tr variants={staggerContainer} initial="hidden" animate="visible">
                {children}
              </motion.tr>
            </tbody>
          )}
        </table>
      </div>
    </div>
  );
};

DataTable.displayName = "DataTable";

/**
 * Table head component.
 */
const TableHead = <T,>({ columns }: { columns: DataTableColumn<T>[] }) => (
  <thead>
    <tr className="bg-neutral-50 border-b border-neutral-100">
      {columns.map((col) => (
        <th
          key={String(col.key)}
          className="px-4 py-3 text-left font-geist text-xs uppercase tracking-wide text-neutral-500 font-medium"
        >
          {col.label}
        </th>
      ))}
    </tr>
  </thead>
);

/**
 * Table body component.
 */
export const DataTableBody = ({
  children,
  ...props
}: React.HTMLAttributes<HTMLTableSectionElement>) => (
  <tbody {...props}>
    <motion.div variants={staggerContainer} initial="hidden" animate="visible">
      {children}
    </motion.div>
  </tbody>
);

DataTableBody.displayName = "DataTableBody";

/**
 * Table row component.
 */
export const DataTableRow = ({
  children,
  className,
  ...props
}: React.HTMLAttributes<HTMLTableRowElement>) => (
  <motion.tr
    className={clsx(
      "border-b border-neutral-100 hover:bg-neutral-50 transition-colors",
      className
    )}
    variants={staggerItem}
    {...(props as any)}
  >
    {children}
  </motion.tr>
);

DataTableRow.displayName = "DataTableRow";

/**
 * Table cell component.
 */
export const DataTableCell = ({
  children,
  className,
  ...props
}: React.HTMLAttributes<HTMLTableCellElement>) => (
  <td className={clsx("px-4 py-3 font-geist text-sm text-neutral-900", className)} {...props}>
    {children}
  </td>
);

DataTableCell.displayName = "DataTableCell";
