// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  ssr: false,
  devtools: { enabled: true },
  extends: ["../tailwind-components"],
  tailwindcss: {
    cssPath: '../tailwind-components/assets/css/main.css',
    configPath: '../tailwind-components/tailwind.config.js'
  },
  routeRules: process.env.API_URL ? { "/graphql/**": { proxy: process.env.API_URL } } : undefined,
  vite: {
    base: ""
  }
})
