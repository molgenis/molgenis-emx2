import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { viteBase } from "../vite-base.js";

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  base: viteBase("updownload", command),
  server: {
    proxy: require("../dev-proxy.config"),
  },
}));
