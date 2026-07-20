import { defineConfig } from "vite";
import { fileURLToPath } from "url";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";
import path from "path";
import vue from "@vitejs/plugin-vue";

dotenv.config({ path: "./.env" });

const dir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ command }) => {
  return {
    resolve: {
      alias: {
        viz: path.resolve(dir, "node_modules/molgenis-viz/src"),
        vizdist: path.resolve(dir, "node_modules/molgenis-viz/dist"),
        molgenis: path.resolve(dir, "node_modules/molgenis-components/dist"),
        nes: path.resolve(dir, "src/styles"),
      },
    },
    plugins: [vue()],
    base: ["dev", "serve"].includes(command) ? "/" : "apps/nestor-public/",
    server: {
      proxy: devProxy,
    },
  };
});
