import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: "apps/tasks/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
});
