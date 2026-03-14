"use client";

import React from "react";
import { ChevronDown } from "lucide-react";
import clsx from "clsx";

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
}

/**
 * Select component with styled native select and error state.
 * @param label - Label text
 * @param error - Error message to display
 */
export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, className, ...props }, ref) => {
    return (
      <div>
        {label && <label className="block text-sm font-medium text-neutral-700 mb-2 font-geist">{label}</label>}

        <div className="relative">
          <select
            ref={ref}
            className={clsx(
              "w-full h-11 px-4 pr-10 border-2 rounded-lg font-geist appearance-none transition-colors",
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
            {...props}
          />

          <ChevronDown 
            className={clsx(
              "absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none",
              props.disabled ? "text-neutral-300" : "text-neutral-400"
            )} 
            size={18} 
          />
        </div>

        {error && <p className="text-red-600 text-sm mt-1 font-geist">{error}</p>}
      </div>
    );
  }
);

Select.displayName = "Select";
