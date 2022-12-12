/* eslint-disable no-undef */
/** @type {import('tailwindcss').Config} */
import typography from "@tailwindcss/typography"
import forms from "@tailwindcss/forms"
export default  {
  content: [
    "./components/**/*.{js,vue,ts}",
    "./layouts/**/*.vue",
    "./pages/**/*.vue",
    "./plugins/**/*.{js,ts}",
    "./nuxt.config.{js,ts}",
    "./app.vue",
  ],
  safelist: [
    {
      pattern: /^bg-/,
    },
    {
      pattern: /^text-/,
    },
    {
      pattern: /^border-/,
    },
    {
      pattern: /^antialiased/,
    },
  ],
  theme: {
    fontFamily: {
      display: ["Bebas Neue", "sans-serif"],
      sans: ["IBM Plex Sans", "sans-serif"],
      mono: "monospace",
    },
    fontSize: {
      "heading-7xl": ["3.75rem", "1.2"],
      "heading-6xl": ["3rem", "1.2"],
      "heading-5xl": ["2.1875rem", "1.2"],
      "heading-4xl": ["1.875rem", "1.2"],
      "heading-3xl": ["1.5625rem", "1.2"],
      "heading-2xl": ["1.375rem", "1.2"],
      "heading-xl": ["1.25rem", "1.2"],
      "heading-lg": ["1.125rem", "1.2"],
      "heading-base": ["1rem", "1.2"],
      "heading-sm": ["0.875rem", "1.2"],
      "body-lg": ["1.125rem", "1.8"],
      "body-base": ["1rem", "1.8"],
      "body-sm": ["0.875rem", "1.8"],
      "body-xs": ["0.75rem", "1.5"],
    },
    colors: {
      current: "currentColor",
      black: "#333",
      white: "#fff",
      transparent: "transparent",
      blue: {
        50: "#E1F0FF",
        100: "#ADD6FF",
        200: "#8BC5FF",
        300: "#53A9FF",
        500: "#017FFD",
        700: "#1D4ED8",
        800: "#014F9E",
      },
      gray: {
        100: "#F4F4F4",
        200: "#E2E2E2",
        400: "#C0C0C0",
        600: "#6D6D6D",
        900: "#1D1D1D",
      },
      yellow: {
        200: "#FFF4CB",
        500: "#FFF500",
        800: "#C89B00",
      },
      green: {
        500: "#72F6B2",
        800: "#349D63",
      },
      orange: {
        500: "#E1B53E",
      },
      red: {
        500: "#E14F62",
      },
    },
    maxWidth: {
      lg: "88.75rem", // 970+380+30+20+20
      tooltip: "10.5rem",
      none: "none",
    },
    extend: {
      boxShadow: {
        primary: "0px 10px 20px rgba(0, 0, 0, 0.1)",
        "search-input": "none",
        "pagination-gray": "0px 10px 20px rgba(0, 0, 0, 0.1)",
      },
      spacing: {
        "3px": "3px",
        "50px": "50px",
        18: "4.5rem",
        19: "4.75rem",
        7.5: "1.875rem",
        8.75: "2.1875rem",
        10.5: "2.625rem",
        12.5: "3.125rem",
        15: "3.75rem",
        25: "6.25rem",
        95: "23.75rem",
        82.5: "20.625rem",
      },
      backgroundImage: {
        "sidebar-gradient":
          "linear-gradient(180deg, #0164C7 0%, rgba(1, 100, 199, 0) 86.02%)",
        "base-gradient": "linear-gradient(180deg, #017FFD 0%, #0163C6 133.81%)",
      },
      backgroundColor: ({ theme }) => ({
        "button-primary": theme("colors.yellow.500"),
        "button-primary-hover": theme("colors.blue.200"),
        "button-secondary": theme("colors.blue.800"),
        "button-secondary-hover": theme("colors.blue.300"),
        "button-tertiary": theme("colors.blue.500"),
        "button-tertiary-hover": theme("colors.blue.300"),
        "button-outline": theme("colors.white"),
        "button-outline-hover": theme("colors.blue.50"),
        "button-disabled": theme("colors.gray.100"),
        "button-disabled-hover": theme("colors.gray.100"),
        "search-button": theme("colors.blue.50"),
        "search-button-hover": theme("colors.blue.700"),
        navigation: "rgba(0, 0, 0, .2)",

        "search-results-view-tabs": theme("colors.blue.800"),

        "search-filter-group-toggle": theme("colors.blue.800"),

        "app-wrapper": theme("colors.black"),

        pagination: theme("colors.blue.800"),
        "pagination-hover": theme("colors.blue.900"),
      }),
      textColor: ({ theme }) => ({
        "button-primary": theme("colors.gray.900"),
        "button-primary-hover": theme("colors.gray.900"),
        "button-secondary": theme("colors.white"),
        "button-secondary-hover": theme("colors.white"),
        "button-tertiary": theme("colors.white"),
        "button-tertiary-hover": theme("colors.white"),
        "button-outline": theme("colors.blue.500"),
        "button-outline-hover": theme("colors.blue.700"),
        "button-disabled": theme("colors.gray.600"),
        "button-disabled-hover": theme("colors.gray.600"),

        menu: theme("colors.white"),
        "sub-menu": theme("colors.blue.500"),
        "sub-menu-hover": theme("colors.blue.700"),
        "breadcrumb-arrow": theme("colors.white"),
        breadcrumb: theme("colors.blue.50"),
        title: theme("colors.white"),
        "search-button": theme("text.blue.500"),
        "search-button-hover": theme("text.blue.800"),

        favorite: theme("colors.white"),
        "favorite-hover": theme("colors.white"),

        "search-results-view-tabs": theme("colors.blue.500"),
        "search-results-view-tabs-hover": theme("colors.blue.800"),

        "search-filter-title": theme("colors.white"),
        "search-filter-expand": theme("colors.yellow.500"),
        "search-filter-group-title": theme("colors.white"),
        "search-filter-group-checkbox": theme("colors.yellow.500"),
        "search-filter-group-toggle": theme("colors.white"),

        "tooltip-hover-dark": theme("colors.blue.500"),
        "tooltip-hover-light": theme("colors.white"),

        pagination: theme("colors.white"),
        "pagination-input": theme("colors.blue.800"),
        "pagination-hover": "#fff",
        "pagination-label-white": theme("colors.white"),
        "pagination-label-gray": theme("colors.gray.400"),
      }),
      borderColor: ({ theme }) => ({
        "button-primary": theme("colors.yellow.500"),
        "button-primary-hover": theme("colors.blue.200"),
        "button-secondary": theme("colors.blue.800"),
        "button-secondary-hover": theme("colors.blue.300"),
        "button-tertiary": theme("colors.blue.500"),
        "button-tertiary-hover": theme("colors.blue.300"),
        "button-outline": theme("colors.blue.500"),
        "button-outline-hover": theme("colors.blue.700"),
        "button-disabled": theme("colors.gray.100"),
        "button-disabled-hover": theme("colors.gray.100"),

        "menu-active": theme("colors.blue.500"),
        "search-button": theme("colors.white"),
        "search-input": theme("colors.white"),

        pagination: "transparent",
      }),
      borderRadius: {
        "3px": "3px",
        "50px": "50px",
        "search-input": "9999px",
        "search-button": "9999px",
        pagination: "9999px",
      },
      opacity: {
        "background-gradient": 100,
      },
    },
  },
  plugins: [typography, forms],
};
