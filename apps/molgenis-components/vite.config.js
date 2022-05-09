import { defineConfig } from "vite";
import { createVuePlugin } from "vite-plugin-vue2";
import path from "path";
import docTagPlugin from "./docs-plugin.js";

const BACKEND_LOCATION = process.env.PROXY_API || "http://localhost:8080/";

// basic build conf fo both library and showCase builds
let conf = {
  plugins: [docTagPlugin(), createVuePlugin()],
  resolve: {
    alias: {
      "vue": require.resolve('vue/dist/vue.js')
    }
  },
  server: {
    proxy: {
      "^/graphql": {
        target: `${BACKEND_LOCATION}/api`,
        changeOrigin: true,
        secure: false,
      },
      "^/.*/graphql$": {
        target: `${BACKEND_LOCATION}`,
        changeOrigin: true,
        secure: false,
      },
      "/apps/central/theme.css": {
        target: `${BACKEND_LOCATION}`,
        changeOrigin: true,
        secure: false,
      },
      "^/apps/resources/webfonts/.*": {
        target: `${BACKEND_LOCATION}`,
        changeOrigin: true,
        secure: false,
      },
    },
  },
};
// In case the SHOW_CASE flag is not set to 'on' build in library mode ( i.e. lib mode is the default)
if (process.env.SHOW_CASE !== "on") {
  console.log("prod build in library mode");
  conf.build = {
    lib: {
      entry: path.resolve(__dirname, "lib/main.js"),
      name: "MolgenisComponents",
      fileName: (format) => `molgenis-components.${format}.js`,
    },
    rollupOptions: {
      // make sure to externalize deps that shouldn't be bundled
      // into your library
      external: ["vue"],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          vue: "Vue",
        },
      },
    },
  };
} else {
  console.log("prod build in show case mode");
  conf.build = {
    outDir: "./showCase",
  };

  conf.base = ""; // use relative base path for use in public_html/app folder
}

// https://vitejs.dev/config/
export default defineConfig(conf);
