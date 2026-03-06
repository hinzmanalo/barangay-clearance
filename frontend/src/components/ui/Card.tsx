"use client";

import React from "react";
import clsx from "clsx";

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  accentColor?: "blue" | "teal" | "amber" | "red" | "green";
  hover?: boolean;
}

/**
 * Card component with optional accent color and hover effects.
 * @param accentColor - Left border color accent
 * @param hover - Enable hover shadow effect
 */
export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ accentColor, hover = false, className, ...props }, ref) => {
    const accentColorMap = {
      blue: "border-blue-500",
      teal: "border-teal-500",
      amber: "border-amber-500",
      red: "border-red-500",
      green: "border-green-500",
    };

    return (
      <div
        ref={ref}
        className={clsx(
          "bg-white rounded-lg p-4",
          accentColor && `border-l-4 ${accentColorMap[accentColor]}`,
          hover && "hover:shadow-md transition-shadow cursor-pointer",
          className
        )}
        {...props}
      />
    );
  }
);

Card.displayName = "Card";
