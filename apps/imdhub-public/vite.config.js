import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(() => {
  require("dotenv").config({ path: `./.env` });

  return {
    css: {
      preprocessorOptions: {
        scss: {
          // @import "src/styles/variables.scss";
          additionalData: `
          @import "../molgenis-viz/src/styles/palettes.scss";
          @import "../molgenis-viz/src/styles/variables.scss";
          @import "../molgenis-viz/src/styles/mixins.scss";
          @import "../molgenis-viz/src/styles/resets.scss";
        `,
        },
      },
    },
    plugins: [vue()],
    base: "",
    server: {
      proxy: require("../dev-proxy.config"),
    },
  };
});
