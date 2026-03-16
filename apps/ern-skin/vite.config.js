import { defineConfig } from "vite";
import path from "path";
import vue from "@vitejs/plugin-vue";
import dotenv from "dotenv";
import devProxy from "./dev-proxy.config";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    resolve: {
      alias: {
        viz: path.resolve(__dirname, "node_modules/molgenis-viz/src"),
        vizdist: path.resolve(__dirname, "node_modules/molgenis-viz/dist"),
        skin: path.resolve(__dirname, "src/styles"),
        molgenis: path.resolve(
          __dirname,
          "node_modules/molgenis-components/dist"
        ),
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
           @import "skin/variables.scss";
           @import "skin/index.scss";
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
