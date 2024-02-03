// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  modules: ["@nuxt/image"],
  devtools: { enabled: true },
  runtimeConfig: {
    public: {
      emx2Theme: "",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      cohortOnly: false,
      proxyTarget: process.env.NUXT_PUBLIC_PROXY_TARGET || "https://data-catalogue.molgeniscloud.org/",
    },
  },
  imports: {
    transform: {
      // exclude
      exclude: [/\bmeta-data-utils\b/],
    },
  },
});
