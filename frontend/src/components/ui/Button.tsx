"use client";

import React from "react";
import { motion } from "framer-motion";
import { Loader2 } from "lucide-react";
import clsx from "clsx";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "ghost" | "danger" | "outline";
  size?: "sm" | "md" | "lg";
  loading?: boolean;
}

/**
 * Button component with support for multiple variants, sizes, and loading state.
 * @param variant - Visual style variant (primary, secondary, ghost, danger, outline)
 * @param size - Button size (sm, md, lg)
 * @param loading - Show loading spinner and disable button
 */
export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { variant = "primary", size = "md", loading = false, className, children, disabled, ...props },
    ref
  ) => {
    const baseStyles =
      "font-geist font-medium transition-all duration-200 flex items-center justify-center gap-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed";

    const variantStyles = {
      primary: "bg-primary-700 text-white hover:bg-primary-800 focus:ring-primary-500",
      secondary: "bg-neutral-100 text-neutral-900 hover:bg-neutral-200 focus:ring-neutral-400",
      ghost: "bg-transparent text-neutral-700 hover:bg-neutral-100 focus:ring-neutral-300",
      danger: "bg-red-600 text-white hover:bg-red-700 focus:ring-red-500",
      outline: "border-2 border-neutral-300 text-neutral-700 hover:border-neutral-400 focus:ring-neutral-300",
    };

    const sizeStyles = {
      sm: "h-9 px-3 text-sm",
      md: "h-11 px-4 text-base",
      lg: "h-12 px-6 text-lg",
    };

    return (
      <motion.button
        ref={ref}
        className={clsx(baseStyles, variantStyles[variant], sizeStyles[size], className)}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        disabled={disabled || loading}
        {...(props as any)}
      >
        {loading ? (
          <>
            <motion.div animate={{ rotate: 360 }} transition={{ repeat: Infinity, duration: 1, ease: "linear" }}>
              <Loader2 size={18} />
            </motion.div>
            {children}
          </>
        ) : (
          children
        )}
      </motion.button>
    );
  }
);

Button.displayName = "Button";
