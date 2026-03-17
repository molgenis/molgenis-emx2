import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import dotenv from "dotenv";
import devProxy from "../dev-proxy.config";
import path from "path";
import { fileURLToPath } from "url";

const dir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    resolve: {
      alias: {
        viz: path.resolve(dir, "node_modules/molgenis-viz/src"),
        vizdist: path.resolve(dir, "node_modules/molgenis-viz/dist"),
        molgenis: path.resolve(dir, "node_modules/molgenis-components/dist"),
        ern: path.resolve(dir, "src/styles"),
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
           @import "viz/styles/heightwidth.scss";
           @import "viz/styles/mixins.scss";
           @import "viz/styles/padding.scss";
           @import "viz/styles/palettes.scss";
           @import "viz/styles/resets.scss";
           @import "viz/styles/textPosition.scss";
           @import "viz/styles/variables.scss";
           @import "molgenis/molgenis-components.css";
           @import "vizdist/molgenis-viz.css";
           @import "ern/variables.scss";
           @import "ern/index.scss";
        `,
        },
      },
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/ern-skin/",
    server: {
      proxy: devProxy,
    },
  };
});
