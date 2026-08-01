import { createRequire } from "node:module";
import { resolve } from "node:path";
import { defineNuxtConfig } from "nuxt/config";
import {
  removePlaygroundLayout,
  removePlaygroundPages,
} from "../tailwind-components/playground";

const monacoEsmDirectory = resolve(
  createRequire(import.meta.url).resolve("monaco-editor/index"),
  "../.."
);

const isProductionBuild = process.env.NODE_ENV === "production";

export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  ssr: false,
  devtools: { enabled: !isProductionBuild },
  telemetry: false,
  runtimeConfig: {
    logLevel: 4,
  },

  tailwindcss: {
    cssPath: "../tailwind-components/app/assets/css/main.css",
    configPath: "../tailwind-components/tailwind.config.js",
  },
  modules: ["@pinia/nuxt", "nuxt-monaco-editor"],

  nitro: {
    publicAssets: [
      {
        dir: monacoEsmDirectory,
        baseURL: "_nuxt/nuxt-monaco-editor",
      },
    ],
  },

  hooks: {
    "pages:extend": removePlaygroundPages,
    "app:resolve": removePlaygroundLayout,
  },
});
