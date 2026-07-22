import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";

dotenv.config({ path: "./.env" });

const dir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ command }) => {
  return {
    resolve: {
        alias: {
            viz: path.resolve(dir, "node_modules/molgenis-viz/src"),
            vizdist: path.resolve(dir, "node_modules/molgenis-viz/dist"),
            molgenis: path.resolve(dir, "node_modules/molgenis-components/dist"),
        },
    },
    plugins: [vue()],
    base: ['dev','serve'].includes(command) ? "/" : "apps/cranio-provider/",
    server: {
      proxy: devProxy,
    },
  };
});
