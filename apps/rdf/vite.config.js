import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ command }) => {
  require("dotenv").config({ path: `./.env` });
  
  return {
   plugins: [vue()],
   base: command === "serve" ? "/" : "apps/rdf/",
   server: {
    proxy: require("../dev-proxy.config"),
  },
  }
})