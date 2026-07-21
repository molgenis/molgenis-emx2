import { strictDevServerPort } from "../dev-env.js";

export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  ssr: false,
  devtools: { enabled: true },
  devServer: {
    port: strictDevServerPort("MOLGENIS_PORT_APP_UI", 3000),
  },
  runtimeConfig: {
    logLevel: 4,
  },

  tailwindcss: {
    cssPath: "../tailwind-components/app/assets/css/main.css",
    configPath: "../tailwind-components/tailwind.config.js",
  },
  modules: ["@pinia/nuxt"],
});
