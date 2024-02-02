// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig, type NuxtConfig } from "nuxt/config";

const config: NuxtConfig = {
  modules: ["@nuxt/image"],
  routeRules: {
    "**/*/graphql": { 
      proxy: 
      { 
        to: "https://data-catalogue.molgeniscloud.org/", 
      } },
      
  },
  devtools: { enabled: true },
  runtimeConfig: {
    // Keys within public, will be also exposed to the client-side
    public: {
      apiBase: "http://localhost:3000/", //'https://emx2.molgeniscloud.org/',
      emx2Theme: "",
      emx2Logo: "",
      siteTitle: "MOLGENIS",
      analyticsKey: "",
      cohortOnly: false,
    },
  },
  // nitro: {
  //   devProxy: {
  //     "/graphql": {
  //       target: "https://data-catalogue.molgeniscloud.org/",
  //       changeOrigin: true,
  //       secure: false,
  //     },
  //   },
  // },
  imports: {
    transform: {
      // exclude
      exclude: [/\bmeta-data-utils\b/],
    },
  },
};

export default defineNuxtConfig(config);
