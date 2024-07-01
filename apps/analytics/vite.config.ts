import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      "^/[a-zA-Z0-9_.%-]+/api/trigger": "http://localhost:8080/",
    }
  }
});
