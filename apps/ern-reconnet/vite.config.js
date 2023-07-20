import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  base: "",
  server: {
    proxy: require("../dev-proxy.config"),
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `
          @import "../../molgenis-viz/src/styles/palettes.scss";
          @import "../../molgenis-viz/src/styles/mixins.scss";
          @import "./src/styles/index.scss";
        `
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        assetFileNames: (assetInfo) => {
          const extension = assetInfo.name.split('.').pop()
          if (/png|jpg|svg/.test(extension)) {
            return `img/[name].[hash][extname]`
          }
          return `${extension}/[name].[hash][extname]`
        },
        chunkFileNames: 'js/[name].[hash].js',
        entryFileNames: 'js/[name].[hash].js'
      }
    }
  }
});
