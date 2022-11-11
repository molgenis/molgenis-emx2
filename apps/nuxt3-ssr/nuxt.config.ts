// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from 'nuxt/config'

const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

export default defineNuxtConfig({
  css: ["assets/css/main.css"],
  modules: ['nuxt-proxy', '@nuxt/image-edge', '@nuxtjs/tailwindcss'],
  runtimeConfig: {
    // Keys within public, will be also exposed to the client-side
    public: {
      apiBase: 'https://emx2.molgeniscloud.org/'
    }
  },
  // proxy: {
  //   options: {
  //     target: process.env.PROXY_TARGET || 'http://localhost:8080/',
  //     pathFilter: [
  //       '**/*/graphql'
  //     ],
  //     changeOrigin: true, secure: false
  //   }
  // },

})

