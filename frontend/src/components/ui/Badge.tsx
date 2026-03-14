"use client";

import React from "react";
import { Dot } from "lucide-react";
import clsx from "clsx";

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant: "status" | "payment" | "role" | "user-status";
  value: string;
  dot?: boolean;
}

type StatusValue = "DRAFT" | "FOR_APPROVAL" | "APPROVED" | "REJECTED" | "RELEASED";
type PaymentValue = "UNPAID" | "PAID" | "WAIVED";
type RoleValue = "ADMIN" | "CLERK" | "APPROVER" | "RESIDENT";
type UserStatusValue = "ACTIVE" | "INACTIVE" | "PENDING_VERIFICATION" | "REJECTED" | "DEACTIVATED";

const statusMap: Record<StatusValue, { label: string; bg: string; text: string; color: string }> = {
  DRAFT: { label: "Draft", bg: "bg-neutral-100", text: "text-neutral-700", color: "#6b7280" },
  FOR_APPROVAL: { label: "For Approval", bg: "bg-amber-100", text: "text-amber-800", color: "#d97706" },
  APPROVED: { label: "Approved", bg: "bg-green-100", text: "text-green-800", color: "#059669" },
  REJECTED: { label: "Rejected", bg: "bg-red-100", text: "text-red-800", color: "#dc2626" },
  RELEASED: { label: "Released", bg: "bg-blue-100", text: "text-blue-800", color: "#2563eb" },
};

const paymentMap: Record<PaymentValue, { label: string; bg: string; text: string; color: string }> = {
  UNPAID: { label: "Unpaid", bg: "bg-orange-100", text: "text-orange-800", color: "#ea580c" },
  PAID: { label: "Paid", bg: "bg-green-100", text: "text-green-800", color: "#16a34a" },
  WAIVED: { label: "Waived", bg: "bg-purple-100", text: "text-purple-800", color: "#7c3aed" },
};

const roleMap: Record<RoleValue, { label: string; bg: string; text: string; color: string }> = {
  ADMIN: { label: "Admin", bg: "bg-red-100", text: "text-red-800", color: "#dc2626" },
  CLERK: { label: "Clerk", bg: "bg-blue-100", text: "text-blue-800", color: "#2563eb" },
  APPROVER: { label: "Approver", bg: "bg-purple-100", text: "text-purple-800", color: "#7c3aed" },
  RESIDENT: { label: "Resident", bg: "bg-teal-100", text: "text-teal-800", color: "#0d9488" },
};

const userStatusMap: Record<UserStatusValue, { label: string; bg: string; text: string; color: string }> = {
  ACTIVE: { label: "Active", bg: "bg-green-100", text: "text-green-800", color: "#059669" },
  INACTIVE: { label: "Inactive", bg: "bg-gray-100", text: "text-gray-700", color: "#6b7280" },
  PENDING_VERIFICATION: { label: "Pending Verification", bg: "bg-yellow-100", text: "text-yellow-800", color: "#d97706" },
  REJECTED: { label: "Rejected", bg: "bg-red-100", text: "text-red-800", color: "#dc2626" },
  DEACTIVATED: { label: "Deactivated", bg: "bg-gray-100", text: "text-gray-600", color: "#6b7280" },
};

/**
 * Badge component that maps status, payment, or role values to styled badges.
 * @param variant - Type of badge (status, payment, or role)
 * @param value - The enum value to display
 * @param dot - Show a colored dot before the label
 */
export const Badge = React.forwardRef<HTMLDivElement, BadgeProps>(
  ({ variant, value, dot = false, className, ...props }, ref) => {
    let config;

    if (variant === "status") {
      config = statusMap[value as StatusValue];
    } else if (variant === "payment") {
      config = paymentMap[value as PaymentValue];
    } else if (variant === "user-status") {
      config = userStatusMap[value as UserStatusValue];
    } else {
      config = roleMap[value as RoleValue];
    }

    if (!config) {
      return <div>Unknown {variant}</div>;
    }

    return (
      <div
        ref={ref}
        className={clsx(
          "inline-flex items-center gap-2 px-2 py-1 rounded text-sm font-medium",
          config.bg,
          config.text,
          className
        )}
        {...props}
      >
        {dot && <Dot size={16} style={{ color: config.color }} fill={config.color} />}
        {config.label}
      </div>
    );
  }
);

Badge.displayName = "Badge";
