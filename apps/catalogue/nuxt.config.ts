import { defineNuxtConfig } from "nuxt/config";
import { apiBase, strictDevServerPort } from "../dev-env.js";

export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  devtools: { enabled: true },
  devServer: {
    port: strictDevServerPort("MOLGENIS_PORT_APP_CATALOGUE", 3000),
  },
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
