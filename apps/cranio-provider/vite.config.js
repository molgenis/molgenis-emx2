import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "NL1";
const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

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
      // proxy: require("../dev-proxy.config"),
      proxy: {
        "/graphql": {
          target: `${HOST}/${SCHEMA}`,
          ...opts,
        },
        // "^/.*/graphql$": {
        //   target: `${HOST}/${SCHEMA}`,
        //   changeOrigin: true,
        //   secure: false,
        // },
        "/apps/central/theme.css": {
          target: `${HOST}/${SCHEMA}`,
          changeOrigin: true,
          secure: false,
        },
        /* should match only '/schema_name/graphql', previous ** was to eager also matching if graphql was /graphql or /a/b/graphql */
        "^/[a-zA-Z0-9_.%-]+/graphql": {
          target: `${HOST}`,
          ...opts,
        },
        "/api": { target: `${HOST}`, ...opts },
        "/apps": { target: `${HOST}`, ...opts },
        "/theme.css": { target: `${HOST}/${SCHEMA}`, ...opts },
      }
    },
  }
});
