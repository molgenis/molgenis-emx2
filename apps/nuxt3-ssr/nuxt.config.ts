// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  modules: ["@nuxt/image"],
  devtools: { enabled: true },
  extends: ["../tailwind-components"],
  tailwindcss: {
    cssPath: '../tailwind-components/assets/css/main.css',
    configPath: '../tailwind-components/tailwind.config.js'
  },
  runtimeConfig: {
    public: {
      emx2Theme: "",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      cohortOnly: false,
      apiBase: process.env.NUXT_PUBLIC_API_BASE || "https://umcgresearchdatacatalogue-acc.molgeniscloud.org/",
    },
  },
  imports: {
    transform: {
      // exclude
      exclude: [/\bmeta-data-utils\b/],
    },
  },
});
