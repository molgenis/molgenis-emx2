// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  modules: ["@nuxt/image"],
  devtools: { enabled: true },
  runtimeConfig: {
    // Keys within public, will be also exposed to the client-side
    public: {
      apiBase: "http://localhost:3000/", //'https://emx2.molgeniscloud.org/',
      emx2Theme: "",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      cohortOnly: false,
      proxyTarget: process.env.PROXY_TARGET || "https://data-catalogue.molgeniscloud.org/",
    },
  },
  imports: {
    transform: {
      // exclude
      exclude: [/\bmeta-data-utils\b/],
    },
  },
});
