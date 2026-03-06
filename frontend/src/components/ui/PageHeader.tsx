"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { ChevronLeft } from "lucide-react";
import Link from "next/link";

export interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  backHref?: string;
}

/**
 * Page header component with title, description, and optional back link.
 * @param title - Page title
 * @param description - Optional description text
 * @param actions - Optional action buttons/elements
 * @param backHref - Optional href for back button
 */
export const PageHeader: React.FC<PageHeaderProps> = ({ title, description, actions, backHref }) => {
  const router = useRouter();

  const handleBack = () => {
    if (backHref) {
      router.push(backHref);
    } else {
      router.back();
    }
  };

  return (
    <div className="mb-8">
      {backHref && (
        <motion.button
          onClick={handleBack}
          className="mb-4 text-neutral-600 hover:text-neutral-900 font-geist text-sm flex items-center gap-1"
          whileHover={{ x: -3 }}
          transition={{ duration: 0.2 }}
        >
          <ChevronLeft size={16} />
          <span>Back</span>
        </motion.button>
      )}

      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-sora font-bold text-2xl text-neutral-900 mb-2">{title}</h1>
          {description && <p className="font-geist text-sm text-neutral-500">{description}</p>}
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
    </div>
  );
};

PageHeader.displayName = "PageHeader";
