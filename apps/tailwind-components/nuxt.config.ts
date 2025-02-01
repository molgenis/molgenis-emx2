// https://nuxt.com/docs/api/configuration/nuxt-config
import fs from 'fs';
import { resolve } from 'path';

// Path to the generated sourceCodeMap.json file
const sourceCodeMapPath = resolve("./sourceCodeMap.json");

// Read the file contents
const sourceCodeMap = JSON.parse(fs.readFileSync(sourceCodeMapPath, 'utf-8'));

export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: [
      "@nuxtjs/tailwindcss",
    "@nuxt/test-utils/module",
  ],
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
      sourceCodeMap
    },
  },

  compatibilityDate: "2024-08-23",
});
