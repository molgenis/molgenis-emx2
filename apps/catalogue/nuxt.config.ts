import { defineNuxtConfig } from "nuxt/config";
import { apiBase } from "../dev-env.js";
import {
  removePlaygroundLayout,
  removePlaygroundPages,
} from "../tailwind-components/playground";

export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  devtools: { enabled: true },
  modules: [
    "@nuxt/test-utils/module",
    "nuxt-gtag",
    "@pinia/nuxt",
    "floating-vue/nuxt",
    "@nuxtjs/tailwindcss",
  ],
  tailwindcss: {
    cssPath: "../tailwind-components/app/assets/css/main.css",
    configPath: "../tailwind-components/tailwind.config.js",
  },
  runtimeConfig: {
    public: {
      emx2Theme: "molgenis",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      analyticsProvider: "",
      analyticsDomain: "",
      cohortOnly: false,
      schema: "catalogue-demo",
      apiBase: apiBase("https://emx2.dev.molgenis.org/"),
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
    storesDirs: ["./app/stores/**"],
  },
  hooks: {
    "pages:extend": removePlaygroundPages,
    "app:resolve": removePlaygroundLayout,
  },
  app: {
    head: {
      htmlAttrs: {
        "data-theme": "",
      },
    },
  },
  // @ts-ignore // gtag is not in the types
  gtag: {
    initMode: "manual",
  },
});
