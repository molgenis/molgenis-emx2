// https://v3.nuxtjs.org/api/configuration/nuxt.config
import { defineNuxtConfig, type NuxtConfig } from "nuxt/config";

const devProxy = {
  options: {
    target:
      process.env.PROXY_TARGET || "https://data-catalogue.molgeniscloud.org/", // 'http://localhost:8080/',
    pathFilter: ["**/*/graphql", "**/api/file/**", "**/api/message/**"],
    changeOrigin: true,
    secure: false,
    logLevel: "debug",
  },
};

const config: NuxtConfig = {
  modules: ["nuxt-proxy", "@nuxt/image"],
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
  nitro: {
    compressPublicAssets: { brotli: true },
  },
};

if (process.env.NODE_ENV === "development") {
  config.proxy = devProxy;
}

export default defineNuxtConfig(config);
