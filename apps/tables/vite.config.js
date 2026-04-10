import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  // css: {

  // }
  base: command === "serve" ? "/" : (process.env.VITE_BASE_PATH ?? "") + "/apps/tables/",
  server: {
    proxy: devProxy,
  },
}));