import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: "apps/updownload/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
});
