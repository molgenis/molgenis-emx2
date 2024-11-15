// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  ssr: false,
  devtools: { enabled: true },
  runtimeConfig: {
    logLevel: 1,
  },
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
      path: "../tailwind-components/components",
    },
  ],

  icon: {
    // provider: process.env.NUXT_PUBLIC_IS_SSR === 'false' ? 'server' : undefined,
    customCollections: [
      {
        prefix: 'twc',
        dir: '../tailwind-components/assets/icons'
      },
    ],
    clientBundle: {
      // list of icons to include in the client bundle
      icons: [
        'user',
      ],

      // scan all components in the project and include icons 
      scan: {
        globInclude: ['**/*.vue', '../tailwind-components/**/*.vue'],
      },

      // include all custom collections in the client bundle
      includeCustomCollections: true, 

      // guard for uncompressed bundle size, will fail the build if exceeds
      sizeLimitKb: 256,
    },
  },
})