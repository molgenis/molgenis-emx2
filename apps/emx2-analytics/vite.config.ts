import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'; // Import the 'path' module
import dts from 'vite-plugin-dts'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), dts()],
  server: {
    proxy: require("../dev-proxy.config"),
  },
  build: {
    lib: {
      entry: path.resolve(__dirname, "src/lib/analytics.ts"), 
      name: "analytics",
      fileName: (format) => `analytics.${format}.js`,
    },
    sourcemap: true,
    emptyOutDir: true,
  },
});
