import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import { viteBase } from "../vite-base.js";

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  base: viteBase("schema", command),
  server: {
    proxy: devProxy,
  },
}));
