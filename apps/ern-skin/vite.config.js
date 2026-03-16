import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
<<<<<<< HEAD
import path from "path";
import dotenv from "dotenv";
import { fileURLToPath } from "url";

// needed because __dirname is not available in ESM
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// load env
dotenv.config({ path: "./.env" });
=======
import dotenv from "dotenv";
>>>>>>> 5062ba0cc (chore: update vite to v6)

export default defineConfig((command) => {
  // Load environment variables
  dotenv.config({ path: "./.env" });

  return {
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
        viz: fileURLToPath(
          new URL("./node_modules/molgenis-viz/src/styles", import.meta.url)
        ),
        vixdist: fileURLToPath(
          new URL("./node_modules/molgenis-viz/dist", import.meta.url)
        ),
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          api: "legacy",
          additionalData: `
          @import "viz/palettes.scss";
          @import "viz/variables.scss";
          @import "viz/mixins.scss";
          @import "@/styles/variables.scss";
          @import "@/styles/index.scss";
          @import "vixdist/molgenis-viz.css";
        `,
        },
      },
    },
    plugins: [vue()],
    base: command === "serve" ? "/" : "apps/ern-skin/",
    server: {
      proxy: devProxy,
    },
  };
});
