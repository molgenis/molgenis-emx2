// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  modules: ["@nuxt/image"],
  devtools: { enabled: true },
  extends: ["../tailwind-components"],
  runtimeConfig: {
    public: {
      emx2Theme: "",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      cohortOnly: false,
      apiBase: process.env.NUXT_PUBLIC_API_BASE || "https://data-catalogue-acc.molgeniscloud.org/",
    },
  },
  imports: {
    transform: {
      // exclude
      exclude: [/\bmeta-data-utils\b/],
    },
  },
});
