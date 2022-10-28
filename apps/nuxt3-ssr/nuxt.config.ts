// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
    css: ["assets/css/main.css"],
    modules: ['nuxt-proxy', '@nuxt/image-edge', '@nuxtjs/tailwindcss'],
    // See options here https://github.com/chimurai/http-proxy-middleware#options
    proxy: {
        options: {
            target: 'http://localhost:8080/',
            pathFilter: [
                '**/*/graphql'
            ],
        }
    },
    typescript: {
        tsConfig: {
            "extends": "./.nuxt/tsconfig.json"
        }
    }
})

    