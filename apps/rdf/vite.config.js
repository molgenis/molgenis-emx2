import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
      @import "../molgenis-viz/src/styles/palettes.scss";
      @import "../molgenis-viz/src/styles/variables.scss";
      @import "../molgenis-viz/src/styles/mixins.scss";
      @import "../molgenis-viz/src/styles/resets.scss";
    `,
      },
    },
  },
  base: command === "serve" ? "/" : "apps/rdf/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
}));