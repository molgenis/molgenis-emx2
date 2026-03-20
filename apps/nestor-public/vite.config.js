import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";
const dir = path.dirname(new URL(import.meta.url).pathname);

export default defineConfig((command) => {
  require("dotenv").config({ path: `./.env` });

  return {
    resolve: {
      alias: {
            '@': path.resolve(__dirname, "src"),
      }  
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/nestor-public/",
    server: {
      proxy: require("../dev-proxy.config"),
    },
  };
});
