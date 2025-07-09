import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig((command) => {
  require("dotenv").config({ path: `./.env` });

  return {
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
          @import "../molgenis-viz/src/styles/palettes.scss";
          @import "../molgenis-viz/src/styles/variables.scss";
          @import "../molgenis-viz/src/styles/mixins.scss";
          @import "../molgenis-viz/src/styles/resets.scss";
          @import "src/styles/variables.scss";
        `,
        },
      },
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/nestor-public/",
    server: {
      proxy: require("../dev-proxy.config"),
    },
  };
});
