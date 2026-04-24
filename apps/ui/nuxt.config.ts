export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  ssr: false,
  devtools: { enabled: true },
  runtimeConfig: {
    logLevel: 4,
  },
  app: {
    baseURL: process.env.NUXT_APP_BASE_URL || "/",
  },

  tailwindcss: {
    cssPath: "../tailwind-components/app/assets/css/main.css",
    configPath: "../tailwind-components/tailwind.config.js",
  },
  modules: ["@pinia/nuxt"],
});
