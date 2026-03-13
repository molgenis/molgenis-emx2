import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";

import path from "path";
import { fileURLToPath } from "url";

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const parentDir = path.resolve(currentDir, "../");

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    css: {
      preprocessorOptions: {
        scss: {
          api: "modern-compiler",
          additionalData: `
          @use "${path.join(parentDir,"./molgenis-viz/src/styles/palettes.scss")}" as *;
          @use "${path.join(parentDir,"./molgenis-viz/src/styles/variables.scss")}" as *; 
          @use "${path.join(parentDir,"./molgenis-viz/src/styles/mixins.scss")}" as *; 
          @use "./src/styles/index.scss" as *;
        `,
        },
      },
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/ern-genturis/",
    server: {
      proxy: devProxy,
    },
  };
});
