import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: "apps/tables/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
});
