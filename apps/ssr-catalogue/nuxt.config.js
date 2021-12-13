const BACKEND_LOCATION = process.env.PROXY_API || 'http://localhost:8080/'

export default {
  // Global page headers: https://go.nuxtjs.dev/config-head
  head: {
    title: "ssr-catalogue",
    htmlAttrs: {
      lang: "en"
    },
    meta: [
      { charset: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { hid: "description", name: "description", content: "" },
      { name: "format-detection", content: "telephone=no" }
    ],
    link: [
      { rel: "icon", type: "image/x-icon", href: "/favicon.ico" },
      { rel: "stylesheet", href: "theme.css" }
    ]
  },

  // Global CSS: https://go.nuxtjs.dev/config-css
  css: [],

  // Plugins to run before rendering page: https://go.nuxtjs.dev/config-plugins
  plugins: [],

  // Auto import components: https://go.nuxtjs.dev/config-components
  components: true,

  // Modules for dev and build (recommended): https://go.nuxtjs.dev/config-modules
  buildModules: [],

  // Modules: https://go.nuxtjs.dev/config-modules
  modules: ["@nuxtjs/axios", "@nuxtjs/proxy"],

  // Build Configuration: https://go.nuxtjs.dev/config-build
  build: {},

  // Axios module configuration: https://go.nuxtjs.dev/config-axios
  axios: {
    proxy: true,
    debug: false,
    baseUrl: `${BACKEND_LOCATION}Catalogue/catalogue`
  },
  proxy: {
    "/theme.css": `${BACKEND_LOCATION}Catalogue/`,
    "/apps/styleguide/assets/img/molgenis_logo_white.png":
      `${BACKEND_LOCATION}`,
    "/graphql": `${BACKEND_LOCATION}Catalogue/catalogue`
  }
};
