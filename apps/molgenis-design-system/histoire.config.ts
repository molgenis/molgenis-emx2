import { defineConfig } from "histoire";
import { HstVue } from "@histoire/plugin-vue";

export default defineConfig({
  plugins: [HstVue()],
  setupFile: "histoire.setup.ts",
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
  theme: {
    title: "Molgenis Design",
    defaultColorScheme: "light",
    favicon: "/molgenis.ico",
    logo: {
      light: "/molgenis_logo.png",
      dark: "/molgenis_logo_white.png"
    }
  }
});
