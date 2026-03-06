"use client";

import React, { useEffect } from "react";
import { motion, useMotionValue, animate, useReducedMotion } from "framer-motion";
import { LucideIcon } from "lucide-react";
import { fadeUp } from "@/lib/animations";

export interface StatCardProps {
  label: string;
  value: number;
  icon: LucideIcon;
  accentColor?: "blue" | "teal" | "amber" | "red" | "green";
  delta?: string;
}

const accentColorMap = {
  blue: { bg: "bg-blue-100", icon: "text-blue-600" },
  teal: { bg: "bg-teal-100", icon: "text-teal-600" },
  amber: { bg: "bg-amber-100", icon: "text-amber-600" },
  red: { bg: "bg-red-100", icon: "text-red-600" },
  green: { bg: "bg-green-100", icon: "text-green-600" },
};

/**
 * Stat card with animated counter, icon, and optional delta.
 * @param label - Stat label
 * @param value - Numeric value to display
 * @param icon - Lucide icon component
 * @param accentColor - Icon background color
 * @param delta - Optional change indicator (e.g., "+5% from last month")
 */
export const StatCard: React.FC<StatCardProps> = ({
  label,
  value,
  icon: Icon,
  accentColor = "blue",
  delta,
}) => {
  const motionValue = useMotionValue(0);
  const shouldReduceMotion = useReducedMotion();
  const [displayValue, setDisplayValue] = React.useState(0);

  useEffect(() => {
    if (shouldReduceMotion) {
      setDisplayValue(value);
    } else {
      const animation = animate(motionValue, value, {
        duration: 1.2,
        ease: "easeOut",
        onUpdate: (latest) => setDisplayValue(Math.round(latest)),
      });

      return () => animation.stop();
    }
  }, [value, motionValue, shouldReduceMotion]);

  const colors = accentColorMap[accentColor];

  return (
    <motion.div
      className="bg-white rounded-lg p-6 border border-neutral-100"
      variants={fadeUp}
      initial="hidden"
      animate="visible"
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="font-geist text-sm text-neutral-500 mb-2">{label}</p>
          <p className="font-sora font-bold text-3xl text-neutral-900">{displayValue}</p>
          {delta && <p className="font-geist text-xs text-neutral-400 mt-1">{delta}</p>}
        </div>

        <div className={`${colors.bg} rounded-lg p-3`}>
          <Icon className={`${colors.icon}`} size={24} />
        </div>
      </div>
    </motion.div>
  );
};

StatCard.displayName = "StatCard";
