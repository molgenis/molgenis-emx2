import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  base: command === "serve" ? "/" : "apps/tasks/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
}));