// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  devtools: { enabled: true },
  modules: ["@nuxt/image"],
  tailwindcss: {
    cssPath: "../tailwind-components/assets/css/main.css",
    configPath: "../tailwind-components/tailwind.config.js",
  },
  runtimeConfig: {
    public: {
      emx2Theme: "",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      cohortOnly: false,
      apiBase:
        process.env.NUXT_PUBLIC_API_BASE ||
        "http://localhost:8080",
    },
  },
  imports: {
    transform: {
      // exclude
      exclude: [/\bmetadata-utils\b/],
    },
  },
  nitro: {
    prerender: {
      ignore: ["/_tailwind/"],
    },
  },
  app: {
    head: {
      htmlAttrs: {
        "data-theme": "",
      },
    },
  },
  components: [

    {
      path: "../tailwind-components/components",
    },
    {
      path: "../tailwind-components/components/global/icons",
      global: true,
    },
  ],
});
