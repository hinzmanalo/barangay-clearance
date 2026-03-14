"use client";

import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import clsx from "clsx";

export interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

/**
 * Textarea component with floating label pattern and error state.
 * @param label - Floating label text
 * @param error - Error message to display
 */
export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, className, value, onFocus, onBlur, rows = 3, ...props }, ref) => {
    const [isFocused, setIsFocused] = useState(false);
    const [isFilled, setIsFilled] = useState(!!value);

    const handleFocus = (e: React.FocusEvent<HTMLTextAreaElement>) => {
      setIsFocused(true);
      onFocus?.(e);
    };

    const handleBlur = (e: React.FocusEvent<HTMLTextAreaElement>) => {
      setIsFocused(false);
      setIsFilled(!!e.target.value);
      onBlur?.(e);
    };

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setIsFilled(!!e.target.value);
      props.onChange?.(e);
    };

    return (
      <div className="relative">
        {label && (
          <motion.label
            className={clsx(
              "absolute left-4 origin-top-left pointer-events-none font-geist text-sm",
              isFocused || isFilled ? "text-neutral-500 text-xs" : "text-neutral-500"
            )}
            animate={
              isFocused || isFilled
                ? { y: -24, scale: 0.875 }
                : { y: 14, scale: 1 }
            }
            transition={{ duration: 0.2, ease: "easeOut" }}
          >
            {label}
          </motion.label>
        )}

        <textarea
          ref={ref}
          rows={rows}
          className={clsx(
            "w-full px-4 py-3 border-2 rounded-lg font-geist transition-colors placeholder-neutral-400 resize-none",
            props.disabled
              ? "bg-neutral-100 border-neutral-200 text-neutral-500 cursor-not-allowed"
              : clsx(
                  "text-neutral-900 bg-white",
                  error
                    ? "border-red-500 focus:border-red-600 focus:outline-none focus:ring-0"
                    : "border-neutral-300 focus:border-primary-500 focus:outline-none focus:ring-0"
                ),
            className
          )}
          onFocus={handleFocus}
          onBlur={handleBlur}
          onChange={handleChange}
          {...props}
        />

        <AnimatePresence>
          {error && (
            <motion.p
              className="text-red-600 text-sm mt-1 font-geist"
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -4 }}
              transition={{ duration: 0.15 }}
            >
              {error}
            </motion.p>
          )}
        </AnimatePresence>
      </div>
    );
  }
);

Textarea.displayName = "Textarea";
