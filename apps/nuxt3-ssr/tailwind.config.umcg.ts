/* eslint-disable no-undef */
/** @type {import('tailwindcss').Config} */
module.exports = {
  presets: [require("./tailwind.config.ts")],
  theme: {
    extend: {
      boxShadow: {
        primary: "none",
        "search-input": "none",
      },
      backgroundColor: ({ theme }) => ({
        "button-primary": "#FF7D00",
        "button-primary-hover": "#FF7D00",
        "button-secondary": "#003183",
        "button-secondary-hover": "#003183",
        "button-tertiary": "#003183",
        "button-tertiary-hover": "#003183",
        "button-outline": theme("colors.white"),
        "button-outline-hover": theme("colors.white"),
        "button-disabled": theme("colors.gray.100"),
        "button-disabled-hover": theme("colors.gray.100"),
        "search-button": "#FFF",
        navigation: "#fff",

        "search-results-view-tabs": "#003183",

        "search-filter-group-toggle": "#bdcce4",

        "app-wrapper": theme("colors.transparent"),
      }),
      backgroundImage: {
        "sidebar-gradient":
          "linear-gradient(180deg, #FFFFFF -24.76%, rgba(255, 255, 255, 0) 86.02%)",
        "base-gradient": "linear-gradient(180deg, #E3EFF8 0%, #E3EFF8 133.81%)",
      },
      textColor: ({ theme }) => ({
        "button-primary": "#FFFFFF",
        "button-primary-hover": "#FFFFFF",
        "button-secondary": "#FFFFFF",
        "button-secondary-hover": "#FFFFFF",
        "button-tertiary": "#FFFFFF",
        "button-tertiary-hover": "#FFFFFF",
        "button-outline": "#003183",
        "button-outline-hover": "#003183",
        "button-disabled": theme("colors.gray.600"),
        "button-disabled-hover": theme("colors.gray.600"),

        menu: "#003183",
        "sub-menu": "#003183",
        "sub-menu-hover": "#003183",
        "breadcrumb-arrow": "#003183",
        breadcrumb: "#003183",
        title: "#003183",
        "search-button": "#017FFD",
        "search-button-hover": "#017FFD",

        icon: "#FF7D00",

        "search-results-view-tabs": "#003183",
        "search-results-view-tabs-hover": "#003183",

        "search-filter-title": "#003183",
        "search-filter-expand": "#0075FF",
        "search-filter-group-title": "#003183",
        "search-filter-group-checkbox": "#fff",
        "search-filter-group-toggle": "#003183",

        "tooltip-hover-dark": "#003183",
        "tooltip-hover-light": "#003183",
      }),
      borderColor: ({ theme }) => ({
        "button-primary": "#FF7D00",
        "button-primary-hover": "#FF7D00",
        "button-secondary": "#FF7D00",
        "button-secondary-hover": "#FF7D00",
        "button-tertiary": "#003183",
        "button-tertiary-hover": "#003183",
        "button-outline": "#003183",
        "button-outline-hover": "#003183",
        "button-disabled": theme("colors.gray.100"),
        "button-disabled-hover": theme("colors.gray.100"),

        "menu-active": "#FF7D00",
        "search-button": "#CCCCCC",
        "search-input": "#CCCCCC",
      }),
      borderRadius: {
        "search-input": "3px",
        "search-button": "0px 3px 3px 0px",
      },
      opacity: {
        "background-gradient": 0,
      },
    },
  },
};
