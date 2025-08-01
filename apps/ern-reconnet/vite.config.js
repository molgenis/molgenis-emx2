import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig((command) => {
  require("dotenv").config({ path: `./.env` });

  return {
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/ern-reconnet/",
    server: {
      proxy: require("../dev-proxy.config"),
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
            @import "../../molgenis-viz/src/styles/palettes.scss";
            @import "../../molgenis-viz/src/styles/mixins.scss";
            @import "./src/styles/index.scss";
          `
        }
      }
    }
  }
});
