import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  base: "",
  server: {
    proxy: require("../../dev-proxy.config"),
  },
  build: {
    commonjsOptions: {
      transformMixedEsModules: true,
    },
    target: "esnext",
  },
});
