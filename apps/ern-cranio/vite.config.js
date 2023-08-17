import { defineConfig } from "vite";
import { fileURLToPath, URL } from 'node:url';
import vue from "@vitejs/plugin-vue";

export default defineConfig(() => {
  require('dotenv').config({ path: `./.env` });

  return {
    resolve: {
      alias: {
        '$shared': fileURLToPath(new URL('../molgenis-viz/src/', import.meta.url))
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
          @import "../molgenis-viz/src/styles/palettes.scss";
          @import "../molgenis-viz/src/styles/variables.scss";
          @import "../molgenis-viz/src/styles/mixins.scss";
        `
        }
      }
    },
    plugins: [vue()],
    base: "",
    server: {
      proxy: require("../dev-proxy.config"),
    },
  }
});
