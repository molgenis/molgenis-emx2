import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/ern-reconnet/",
    server: {
      proxy: devProxy
    },
    css: {
      preprocessorOptions: {
        scss: {
          api: "legacy",
          additionalData: `
            @import "../molgenis-viz/src/styles/palettes.scss";
            @import "../molgenis-viz/src/styles/mixins.scss";
            @import "src/styles/index.scss";
          `
        }
      }
    }
  }
});
