import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";
const dir = path.dirname(new URL(import.meta.url).pathname);

export default defineConfig((command) => {
  require("dotenv").config({ path: `./.env` });

  return {
    resolve: {
      alias: {
        viz: path.resolve(dir, "node_modules/molgenis-viz/src"),
        vizdist: path.resolve(dir, "node_modules/molgenis-viz/dist"),
        molgenis: path.resolve(dir,"node_modules/molgenis-components/dist"),
        nes: path.resolve(dir, "src/styles"),
      }  
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
          @import "viz/styles/palettes.scss";
          @import "viz/styles/variables.scss";
          @import "viz/styles/mixins.scss";
          @import "viz/styles/resets.scss";
          @import "nes/variables.scss";
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
