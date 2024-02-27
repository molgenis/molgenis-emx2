// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },
  modules: ['@nuxtjs/storybook', '@nuxtjs/tailwindcss', 'floating-vue/nuxt'],
  components: [{ path: "./components", prefix: "UI" }],
})
