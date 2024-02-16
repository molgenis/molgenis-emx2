import { defineConfig } from "histoire"
import { HstVue } from "@histoire/plugin-vue"
import { HstNuxt } from "@histoire/plugin-nuxt"

export default defineConfig({
  setupFile: "./histoire-setup.ts",
  outDir: "showCase",
  routerMode: "hash",
  vite: {
    base: "",
    server: {
      fs: {
        allow: [
          "../node_modules/histoire/dist/node/builtin-plugins/vanilla-support",
          "../node_modules/@histoire/plugin-vue/dist/bundled/client/app",
          "../node_modules/@histoire/controls/dist",
        ],
      },
    },
  },
  plugins: [
    HstVue(),
    HstNuxt(),
  ],
})