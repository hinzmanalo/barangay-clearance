"use client";

import React from "react";
import { motion } from "framer-motion";
import clsx from "clsx";

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  width?: string;
  height?: string;
}

/**
 * Base Skeleton component for inline block elements.
 */
export const Skeleton = React.forwardRef<HTMLDivElement, SkeletonProps>(
  ({ width = "100%", height = "1rem", className, ...props }, ref) => (
    <motion.div
      ref={ref}
      className={clsx("rounded bg-neutral-200", className)}
      style={{ width, height }}
      animate={{ opacity: [0.5, 0.9, 0.5] }}
      transition={{ repeat: Infinity, duration: 1.5 }}
      {...(props as any)}
    />
  )
);

Skeleton.displayName = "Skeleton";

/**
 * Skeleton for text lines with staggered animation.
 */
export const SkeletonText = ({ lines = 3 }: { lines?: number }) => (
  <motion.div
    className="space-y-3"
    initial="hidden"
    animate="visible"
    variants={{
      visible: {
        transition: {
          staggerChildren: 0.05,
        },
      },
    }}
  >
    {Array.from({ length: lines }).map((_, i) => (
      <Skeleton key={i} height="1rem" />
    ))}
  </motion.div>
);

/**
 * Skeleton for table rows.
 */
export const TableRowSkeleton = ({ columns = 5 }: { columns?: number }) => (
  <tr>
    {Array.from({ length: columns }).map((_, i) => (
      <td key={i} className="px-4 py-3">
        <Skeleton height="1rem" />
      </td>
    ))}
  </tr>
);

/**
 * Skeleton for card components.
 */
export const CardSkeleton = () => (
  <div className="bg-white rounded-lg p-4 space-y-3">
    <Skeleton height="1.5rem" width="60%" />
    <SkeletonText lines={2} />
    <Skeleton height="2.5rem" width="40%" />
  </div>
);

/**
 * Skeleton for list pages with multiple cards.
 */
export const ListPageSkeleton = ({ count = 5 }: { count?: number }) => (
  <div className="space-y-3">
    {Array.from({ length: count }).map((_, i) => (
      <CardSkeleton key={i} />
    ))}
  </div>
);

/**
 * Skeleton for detail pages with header and content sections.
 */
export const DetailPageSkeleton = () => (
  <div className="space-y-6">
    {/* Header */}
    <div className="bg-white rounded-lg p-6 space-y-3">
      <Skeleton height="2rem" width="50%" />
      <Skeleton height="1rem" width="70%" />
    </div>

    {/* Content sections */}
    {Array.from({ length: 3 }).map((_, i) => (
      <div key={i} className="bg-white rounded-lg p-6 space-y-3">
        <Skeleton height="1.5rem" width="40%" />
        <SkeletonText lines={3} />
      </div>
    ))}
  </div>
);
