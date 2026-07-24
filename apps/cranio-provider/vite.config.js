import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import devProxy from "../dev-proxy.config";
import path from "path";
import { fileURLToPath } from "url";

const dir = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ command }) => {
  return {
    resolve: {
        alias: {
            viz: path.resolve(dir, "node_modules/molgenis-viz/src"),
            vizdist: path.resolve(dir, "node_modules/molgenis-viz/dist"),
            molgenis: path.resolve(dir, "node_modules/molgenis-components/dist"),
            ern: path.resolve(dir, "src/styles"),
        },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
            @import "viz/styles/palettes.scss";
            @import "viz/styles/variables.scss";
            @import "viz/styles/mixins.scss";
            @import "ern/variables.scss";
            @import "ern/index.scss";
            @import "molgenis/molgenis-components.css";
            @import "vizdist/molgenis-viz.css";
        `,
        },
      },
    },
    plugins: [vue()],
    base: ['dev','serve'].includes(command) ? "/" : "apps/cranio-provider/",
    server: {
      proxy: devProxy,
    },
  };
});
