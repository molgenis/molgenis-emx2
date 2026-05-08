import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";
import { viteBase } from "../vite-base.js";

const dir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({command}) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    resolve: {
        alias: {
            viz: path.resolve(dir, "node_modules/molgenis-viz/src"),
            vizdist: path.resolve(dir, "node_modules/molgenis-viz/dist"),
            molgenis: path.resolve(dir, "node_modules/molgenis-components/dist"),
        },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
            @import "viz/styles/palettes.scss";
            @import "viz/styles/variables.scss";
            @import "viz/styles/mixins.scss";
            @import "molgenis/molgenis-components.css";
            @import "vizdist/molgenis-viz.css";
        `,
        },
      },
    },
    plugins: [vue()],
    base: viteBase("aggregates", command),
    server: {
      proxy: devProxy
    },
  };
});
