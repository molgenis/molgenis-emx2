import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";

import path from "path";
import dotenv from "dotenv";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({ path: "./.env" });

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
        viz: fileURLToPath(
          new URL("./node_modules/molgenis-viz/src/styles", import.meta.url)
        ),
        vixdist: fileURLToPath(
          new URL("./node_modules/molgenis-viz/dist", import.meta.url)
        ),
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
          @import "viz/palettes.scss";
          @import "viz/variables.scss";
          @import "viz/mixins.scss";
          @import "@/styles/variables.scss";
          @import "@/styles/index.scss";
          @import "vixdist/molgenis-viz.css";
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
