// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: [
    "@nuxt/image",
    "@nuxtjs/tailwindcss",
    "floating-vue/nuxt",
    "@nuxt/test-utils/module",
  ],
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
        "https://preview-emx2-pr-4090.dev.molgenis.org/",
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
      path: "~/components",
    },
    {
      path: "../tailwind-components/components",
    },
    {
      path: "../tailwind-components/components/global/icons",
      global: true,
    },
  ],
});
