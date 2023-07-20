import {fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: "",
  server: {
    proxy: require("../dev-proxy.config"),
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
          @import "../../molgenis-viz/src/styles/heightwidth.scss";
          @import "../../molgenis-viz/src/styles/mixins.scss";
          @import "../../molgenis-viz/src/styles/padding.scss";
          @import "../../molgenis-viz/src/styles/palettes.scss";
          @import "../../molgenis-viz/src/styles/resets.scss";
          @import "../../molgenis-viz/src/styles/textPosition.scss";
          @import "../../molgenis-viz/src/styles/variables.scss";
          @import "./src/styles/index.scss";
        `
      }
    }
  }
});
