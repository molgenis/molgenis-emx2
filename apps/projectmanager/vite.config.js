import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig((command) => {
  return {
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/projectmanager/",
    server: {
      proxy: require("../dev-proxy.config"),
    },
  }
});
