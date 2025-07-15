// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from "nuxt/config";

export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  devtools: { enabled: true },
  experimental: {
    watcher: 'parcel'
  },
  modules: ["@nuxt/image", "@nuxt/test-utils/module", "nuxt-gtag", "@pinia/nuxt", "floating-vue/nuxt"],
  ignore: ['.gradle/**', '.git/**', 'node_modules/**', 'dist/**', 'coverage/**'],
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
  },
  //proxy the cms to api base
  vite: {
    server: {
      proxy: {
        // Proxy all requests starting with /cms to your backend
        // todo: test if this fails for big uploads when data managing..., otherwise we must use nginx or something
        '/cms': {
          target: process.env.NUXT_PUBLIC_API_BASE ||
              "https://emx2.dev.molgenis.org/",  // or http://localhost:8080/
          changeOrigin: true,
          rewrite: (path) => {
            // If path matches /cms/<something>/favicon.ico
            const faviconMatch = path.match(/^\/cms\/[^\/]+\/favicon\.ico$/);
            if (faviconMatch) {
              // Rewrite all such requests to root /favicon.ico on backend
              return '/favicon.ico';
            }
            // Otherwise, strip only /cms prefix
            return path.replace(/^\/cms/, '');
          },
        },
        '/apps': {
          target: process.env.NUXT_PUBLIC_API_BASE ||
              "https://emx2.dev.molgenis.org/",  // or http://localhost:8080/
          changeOrigin: true,
        },
      },
    },
  },
});
