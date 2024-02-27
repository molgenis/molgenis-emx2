import { defineConfig } from "vite";
import path from "path";
import vue from "@vitejs/plugin-vue";

const host = "https://emx2.dev.molgenis.org";
const schema = "DataViz";
const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

// basic build conf fo both library
let conf = {
  plugins: [vue()],
  base: "",
  resolve: {
    alias: {
      vue: require.resolve("vue/dist/vue.runtime.esm-bundler.js"),
    },
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
  }
};

export default defineConfig(({ command, mode }) => { 
  
  if (command === 'serve') {
    return {
      ...conf,
      server: {
        proxy: {
          "/api/graphql": {
            target: `${host}/${schema}`,
            ...opts,
          },
          "^/[a-zA-Z0-9_.%-]+/api/graphql": {
            target: host,
            ...opts,
          },
          "/api": {
            target: `${host}/api`,
            ...opts,
          },
          "/graphql": {
            target: `${host}/api/graphql`,
            ...opts,
          },
          "/apps": {
            target: host,
            ...opts,
          },
          "/theme.css": {
            target: `${host}/apps/central`,
            ...opts,
          },
        },
      },
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
              }
            }
          }
        }
      }
    }
    
    if (command === "build" && mode == "app") {
      return {
        ...conf,
        build: {
          rollupOptions: {
            output: {
              assetFileNames: (assetInfo) => {
                const extension = assetInfo.name.split('.').pop()
                if (/png|jpg|svg/.test(extension)) {
                  return `img/[name].[hash][extname]`
                }
                return `${extension}/dashboard.[hash][extname]`
              },
              chunkFileNames: 'js/[name].[hash].js',
              entryFileNames: 'js/[name].[hash].js'
            }
          }
        }
      }
    }
  }
})
