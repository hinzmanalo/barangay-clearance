import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sora: ["var(--font-sora)", "sans-serif"],
        geist: ["var(--font-geist)", "sans-serif"],
      },
      colors: {
        primary: {
          900: "var(--clr-primary-900)",
          800: "var(--clr-primary-800)",
          700: "var(--clr-primary-700)",
          600: "var(--clr-primary-600)",
          500: "var(--clr-primary-500)",
          300: "var(--clr-primary-300)",
          200: "var(--clr-primary-200)",
          100: "var(--clr-primary-100)",
        },
        teal: {
          600: "var(--clr-teal-600)",
          500: "var(--clr-teal-500)",
          400: "var(--clr-teal-400)",
          100: "var(--clr-teal-100)",
        },
        neutral: {
          950: "var(--clr-neutral-950)",
          900: "var(--clr-neutral-900)",
          700: "var(--clr-neutral-700)",
          500: "var(--clr-neutral-500)",
          300: "var(--clr-neutral-300)",
          100: "var(--clr-neutral-100)",
          50: "var(--clr-neutral-50)",
        },
        surface: "var(--clr-surface)",
        border: "var(--clr-border)",
        status: {
          draft: "var(--clr-status-draft)",
          "for-approval": "var(--clr-status-for-approval)",
          approved: "var(--clr-status-approved)",
          rejected: "var(--clr-status-rejected)",
          released: "var(--clr-status-released)",
        },
        pay: {
          unpaid: "var(--clr-pay-unpaid)",
          paid: "var(--clr-pay-paid)",
          waived: "var(--clr-pay-waived)",
        },
      },
      boxShadow: {
        sm: "var(--shadow-sm)",
        md: "var(--shadow-md)",
        lg: "var(--shadow-lg)",
      },
      borderRadius: {
        "2xl": "1rem",
      },
    },
  },
  plugins: [],
};
export default config;
