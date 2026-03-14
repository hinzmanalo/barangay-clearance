"use client";

import React from "react";
import { motion } from "framer-motion";
import { LucideIcon } from "lucide-react";
import Link from "next/link";
import { fadeUp } from "@/lib/animations";
import { Button } from "./Button";

export interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: { label: string; href?: string; onClick?: () => void };
}

/**
 * Empty state component for displaying when no data is available.
 * @param icon - Lucide icon to display
 * @param title - Empty state title
 * @param description - Descriptive text
 * @param action - Optional action button
 */
export const EmptyState: React.FC<EmptyStateProps> = ({ icon: Icon, title, description, action }) => {
  return (
    <motion.div
      className="flex flex-col items-center justify-center py-12 px-4 text-center"
      variants={fadeUp}
      initial="hidden"
      animate="visible"
    >
      <Icon className="text-neutral-300 mb-4" size={48} />
      <h3 className="font-sora font-bold text-lg text-neutral-900 mb-2">{title}</h3>
      <p className="font-geist text-neutral-500 mb-6 max-w-md">{description}</p>

      {action && (
        <>
          {action.href ? (
            <Link href={action.href}>
              <Button variant="primary">{action.label}</Button>
            </Link>
          ) : (
            <Button variant="primary" onClick={action.onClick}>
              {action.label}
            </Button>
          )}
        </>
      )}
    </motion.div>
  );
};

EmptyState.displayName = "EmptyState";
