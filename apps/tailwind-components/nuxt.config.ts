// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: ['@nuxtjs/tailwindcss', 'floating-vue/nuxt', '@nuxt/test-utils/module'],
  tailwindcss: {
    cssPath: '~/assets/css/main.css',
    configPath: '~/tailwind.config.js'
  },
  ssr: process.env.NUXT_PUBLIC_IS_SSR === 'false' ? false : true,
  router: {
    options: process.env.NUXT_PUBLIC_IS_SSR === 'false' ? {
      hashMode: true
    } : {}
  },
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
  components: [
    {
      path: "~/components/global/icons",
      global: true,
    },
    {
      path: "~/components/viz",
      pathPrefix: false
    },
    "~/components",
  ],
  runtimeConfig: {
    public: {
      apiBase: "https://emx2.dev.molgenis.org/", // "http://localhost:8080/",
    },
  },
})
