import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";
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
        molgenis: path.resolve(dir,"node_modules/molgenis-components/dist"),
        '@': path.resolve(__dirname, "src"),
      }  
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/ern-reconnet/",
    server: {
      proxy: devProxy
    },
  }
});
