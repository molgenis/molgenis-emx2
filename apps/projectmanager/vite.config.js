import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(() => {
  require("dotenv").config({ path: `./.env` });
  
  return {   
    plugins: [vue()],
    base: "apps/projectmanager/",
    server: {
      proxy: require("../dev-proxy.config"),
    },
  }
});
