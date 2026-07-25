// https://nuxt.com/docs/api/configuration/nuxt-config
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { defineNuxtConfig } from "nuxt/config";
import * as fs from "fs";
import { resolve } from "path";
import { apiBase } from "../dev-env.js";
import { setPlaygroundLayout } from "./playground";

const sourceCodeMapPath = resolve("./sourceCodeMap.json");
const sourceCodeMap = fs.existsSync(sourceCodeMapPath)
  ? JSON.parse(fs.readFileSync(sourceCodeMapPath, "utf-8"))
  : { none: "none" };

const playgroundIsTheApp =
  resolve(process.cwd()) ===
  resolve(fileURLToPath(new URL(".", import.meta.url)));
const monacoModules = playgroundIsTheApp ? ["nuxt-monaco-editor"] : [];
const monacoPublicAssets = playgroundIsTheApp
  ? [
      {
        dir: resolve(
          createRequire(import.meta.url).resolve("monaco-editor/index"),
          "../.."
        ),
        baseURL: "_nuxt/nuxt-monaco-editor",
      },
    ]
  : [];

export default defineNuxtConfig({
  devtools: { enabled: true },
  experimental: {
    watcher: "parcel",
  },
  modules: [
    "@nuxt/test-utils/module",
    "floating-vue/nuxt",
    "@nuxtjs/tailwindcss",
    ...monacoModules,
  ],
  ignore: [
    ".gradle/**",
    ".git/**",
    "node_modules/**",
    "dist/**",
    "coverage/**",
  ],
  imports: {
    autoImport: false,
  },
  tailwindcss: {
    cssPath: "~/assets/css/main.css",
    configPath: "~/tailwind.config.js",
  },

  ssr: process.env.NUXT_PUBLIC_IS_SSR === "false" ? false : true,

  router: {
    options:
      process.env.NUXT_PUBLIC_IS_SSR === "false"
        ? {
            hashMode: true,
          }
        : {},
  },

  nitro: {
    prerender: {
      ignore: ["/_tailwind/"],
    },
    publicAssets: monacoPublicAssets,
  },

  hooks: {
    "pages:extend": setPlaygroundLayout,
  },

  app: {
    head: {
      htmlAttrs: {
        "data-theme": "",
      },
    },
  },

  runtimeConfig: {
    public: {
      apiBase: apiBase(
        process.env.CI
          ? "http://localhost:8080/"
          : "https://emx2.dev.molgenis.org/"
      ),
      sourceCodeMap: sourceCodeMap,
    },
  },

  compatibilityDate: "2024-08-23",
});
