// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  devtools: { enabled: true },
  ignore: ['.gradle/**', '.git/**', 'node_modules/**', 'dist/**', 'coverage/**'],
  modules: ["@nuxt/image", "@nuxt/test-utils/module", "nuxt-gtag", "@pinia/nuxt"],
  tailwindcss: {
    cssPath: "../tailwind-components/assets/css/main.css",
    configPath: "../tailwind-components/tailwind.config.js",
  },
  runtimeConfig: {
    public: {
      emx2Theme: "molgenis",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      analyticsProvider: "siteimprove",
      cohortOnly: false,
      schema: "catalogue-demo",
      apiBase:
        process.env.NUXT_PUBLIC_API_BASE ||
        "https://emx2.dev.molgenis.org/",
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
  pinia: {
    storesDirs: ['./stores/**'],
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
  // @ts-ignore // gtag is not in the types
  gtag: {
    initMode: 'manual',
  }
});
