import { defineConfig } from "vite";
import { createVuePlugin as vue } from "vite-plugin-vue2";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      vue: require.resolve("vue/dist/vue.js"),
    },
  },
  base: "",
  server: {
    proxy: require("../dev-proxy.config"),
  },
  build: {
    commonjsOptions: {
      transformMixedEsModules: true,
    },
    target: "esnext",
  },
});
