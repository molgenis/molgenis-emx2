// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  ssr: false,
  devtools: { enabled: true },
  extends: ["../tailwind-components"],
  tailwindcss: {
    cssPath: '../tailwind-components/assets/css/main.css',
    configPath: '../tailwind-components/tailwind.config.js'
  },
  // runtimeConfig: {
  //   public: {
  //     apiBase: "https://emx2.dev.molgenis.org/"
  //    // apiBase: "http://localhost:8080/"
  //   },
  // },
  vite: {
    base: "."
  },
  modules: [ '@pinia/nuxt' ],
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
})
