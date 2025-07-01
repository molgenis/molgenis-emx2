// https://nuxt.com/docs/api/configuration/nuxt-config
import fs from "fs";
import { resolve } from "path";

const sourceCodeMapPath = resolve("./sourceCodeMap.json");
const sourceCodeMap = fs.existsSync(sourceCodeMapPath)
  ? JSON.parse(fs.readFileSync(sourceCodeMapPath, "utf-8"))
  : { none: "none" };

export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: [
    "@nuxtjs/tailwindcss",
    "@nuxt/test-utils/module",
    "floating-vue/nuxt",
  ],
  imports: {
    autoImport: false,
  },
  tailwindcss: {
    cssPath: "~/assets/css/main.css",
    configPath: "~/tailwind.config.js",
  },

  ssr: process.env.NUXT_PUBLIC_IS_SSR === "false" ? false : true,

  router: {
    options:
      process.env.NUXT_PUBLIC_IS_SSR === "false"
        ? {
            hashMode: true,
          }
        : {},
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
      path: "~/components/global/icons",
      global: true,
    },
    {
      path: "~/components/viz",
      pathPrefix: false,
    },
    "~/components",
  ],

  runtimeConfig: {
    public: {
      apiBase: "https://emx2.dev.molgenis.org/",
      sourceCodeMap: sourceCodeMap,
    },
  },

  compatibilityDate: "2024-08-23",
});
