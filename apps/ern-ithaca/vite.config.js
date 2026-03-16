import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import dotenv from "dotenv";

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    css: {
      preprocessorOptions: {
        scss: {
<<<<<<< HEAD
          api: "legacy",
=======
          api: 'legacy',
>>>>>>> 5062ba0cc (chore: update vite to v6)
          additionalData: `
          @import "../molgenis-viz/src/styles/palettes.scss";
          @import "../molgenis-viz/src/styles/variables.scss";
          @import "../molgenis-viz/src/styles/mixins.scss";
          @import "src/styles/variables.scss";
        `,
        },
      },
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/ern-ithaca/",
    server: {
      proxy: devProxy
    },
  };
});
