import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const host = "https://emx2.dev.molgenis.org";
const schema = "NL1";
const opts = { changeOrigin: true, secure: false, logLevel: "debug" }

export default defineConfig(() => {
  // require('dotenv').config({ path: `./.env` });
 
  return {
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
          @import "../molgenis-viz/src/styles/palettes.scss";
          @import "../molgenis-viz/src/styles/variables.scss";
          @import "../molgenis-viz/src/styles/mixins.scss";
          @import "src/styles/variables.scss";
        `
        }
      }
    },
    plugins: [vue()],
    base: "",
    server: {
      proxy: {
        "/api/graphql": {
          target: `${host}/${schema}`,
          ...opts
        },
        "^/[a-zA-Z0-9_.%-]+/api/graphql": {
          target: host,
          ...opts,
        },
        "/api": {
          target: `${host}/api`,
          ...opts
        },
        "/apps": {
          target: host,
          ...opts
        },
        "/theme.css": {
          target: `${host}/apps/central`,
          ...opts
        },
      }
    },
  }
});
