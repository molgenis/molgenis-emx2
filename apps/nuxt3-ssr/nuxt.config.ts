// https://v3.nuxtjs.org/api/configuration/nuxt.config

export default defineNuxtConfig({
    css: ["assets/css/main.css"],
    modules: ['nuxt-proxy', '@nuxt/image-edge', '@nuxtjs/tailwindcss'],
    // See options here https://github.com/chimurai/http-proxy-middleware#options
    proxy: {
        options: {
            logLevel: 'debug',
            target: 'http://localhost:8080/',
            changeOrigin: true,
            // pathRewrite: {
            //     '^/api/todos': '/todos',
            //     '^/api/users': '/users'
            // },
            pathFilter: [
                '**/*/graphql'
            ],
        }
    },
    vite: {
        server: {
            proxy: {
                "/graphql": { target: "http://localhost:8080/", changeOrigin: true },
                "/*/graphql": { target: "http://localhost:8080/", changeOrigin: true },
            },
        },
    },
})

    