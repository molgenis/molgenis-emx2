import { defineConfig } from 'vite'
import { createVuePlugin } from "vite-plugin-vue2"
import path from 'path'
import docTagPlugin from './docs-plugin.js'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [docTagPlugin(), createVuePlugin()],
  build: {
    lib: {
      entry: path.resolve(__dirname, "lib/main.js"),
      name: "Components",
      fileName: (format) => `components.${format}.js`,
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
  },
});


