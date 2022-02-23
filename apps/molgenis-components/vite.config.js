import { defineConfig } from 'vite'
import { createVuePlugin } from "vite-plugin-vue2"
import path from 'path'
import docTagPlugin from './docs-plugin.js'

const BACKEND_LOCATION = process.env.PROXY_API || "http://localhost:8080/";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [docTagPlugin(), createVuePlugin()],
  build: {
    lib: {
      entry: path.resolve(__dirname, "lib/main.js"),
      name: "MolgenisComponents",
      fileName: (format) => `molgenis-components.${format}.js`,
    },
    rollupOptions: {
      // make sure to externalize deps that shouldn't be bundled
      // into your library
      external: ["vue", "axios"],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          vue: "Vue",
        },
      },
    },
  },
  server: {
    proxy: {
      "^/.*/graphql": {
        target: `${BACKEND_LOCATION}`,
        changeOrigin: true,
        secure: false,
      },
    },
  },
});


