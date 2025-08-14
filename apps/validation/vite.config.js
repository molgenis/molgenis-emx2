import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  base: command === "serve" ? "/" : "apps/validation/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
}));