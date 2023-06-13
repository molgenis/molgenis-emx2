import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import path from "path";
import vue from "@vitejs/plugin-vue";

const BACKEND_LOCATION = process.env.PROXY_API || "http://localhost:8080/";

// basic build conf fo both library
let conf = {
  plugins: [vue()],
  base: "",
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
          @import "src/styles/heightwidth.scss";
          @import "src/styles/mixins.scss";
          @import "src/styles/padding.scss";
          @import "src/styles/palettes.scss";
          @import "src/styles/resets.scss";
          @import "src/styles/textPosition.scss";
          @import "src/styles/variables.scss";
        `
      }
    }
  },
  server: {
    proxy: {
      "/apps/molgenis-components/assets/img/molgenis_logo_white.png": {
        target: BACKEND_LOCATION,
        changeOrigin: true,
        secure: false,
      },
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

export default defineConfig(({ command, mode }) => {
  if (command === 'serve') {
    return {
      ...conf,
      resolve: {
        alias: {
          '@': fileURLToPath(new URL('./src', import.meta.url)),
          vue: require.resolve("vue/dist/vue.runtime.esm-bundler.js"),
        },
      },
      base: "",
      server: {
        port: 8080
      }
    }
  } else {

    if (command === "build" && mode === "lib") {
      return {
        ...conf,
        build: {
          lib: {
            entry: path.resolve(__dirname, "lib/main.js"),
            name: "molgenis-viz",
            fileName: (format) => `molgenis-viz.${format}.js`,
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
              assetFileNames: (assetInfo) => {
                if (assetInfo.name === 'style.css') {
                  return 'molgenis-viz.css'
                }
                return assetInfo.name
              }
            }
          }
        }
      }
    }
  }
})
