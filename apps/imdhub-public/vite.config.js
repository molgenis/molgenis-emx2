import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const host = "https://demo-recon4imd.molgeniscloud.org";
const schema = "IMDhub Refs";
const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

export default defineConfig(() => {
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
