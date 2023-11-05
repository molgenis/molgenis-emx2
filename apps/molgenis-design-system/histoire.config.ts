import { defineConfig } from "histoire";
import { HstVue } from "@histoire/plugin-vue";

export default defineConfig({
  plugins: [HstVue()],
  setupFile: "histoire.setup.ts",
  outDir: "showCase",
  vite: {
    server: {
      fs: {
        allow: [
          "../node_modules/histoire/dist/node/builtin-plugins/vanilla-support",
          "../node_modules/@histoire/plugin-vue/dist/bundled/client/app",
        ],
      },
    },
  },
});
