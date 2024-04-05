// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss', 'floating-vue/nuxt', '@nuxt/test-utils/module'],
  // components: [{ path: "./components", prefix: "UI" }],
  tailwindcss: {
    cssPath: '~/assets/css/main.css',
    configPath: '~/tailwind.config.js'
  },
  ssr: process.env.NUXT_PUBLIC_IS_SSR === 'false' ? false : true,
  nitro: {
    prerender: {
      ignore: ['/_tailwind/']
    }
  },
  app: {
    head: {
      htmlAttrs: {
        'data-theme': ''
      }
    }
  },
})
