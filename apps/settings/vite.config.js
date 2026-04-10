import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  base: command === "serve" ? "/" : (process.env.VITE_BASE_PATH ?? "") + "/apps/settings/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
}));