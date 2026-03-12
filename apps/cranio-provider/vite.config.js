import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    css: {
      preprocessorOptions: {
        scss: {
          api: 'legacy',
          additionalData: `
          @import "../molgenis-viz/src/styles/palettes.scss";
          @import "../molgenis-viz/src/styles/variables.scss";
          @import "../molgenis-viz/src/styles/mixins.scss";
          @import "src/styles/variables.scss";
        `,
        },
      },
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/cranio-provider/",
    server: {
      proxy: devProxy,
    },
  };
});
