import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const HOST = process.env.MOLGENIS_APPS_HOST || "http://localhost:8080";
const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  base: command === "serve" ? "/" : "apps/hpc/",
  server: {
    proxy: {
      // _SYSTEM_ schema (GraphQL + theme)
      "/_SYSTEM_": { target: HOST, ...opts },
      // HPC REST API
      "/api/hpc": { target: HOST, ...opts },
      // General API (sign-in mutation goes to /api/graphql)
      "/api": { target: HOST, ...opts },
      // Session reload uses /apps/central/graphql
      "/apps": { target: HOST, ...opts },
      // Session reload also uses bare /graphql (relative)
      "/graphql": { target: `${HOST}/_SYSTEM_/graphql`, ...opts },
      // Schema-scoped graphql
      "^/[a-zA-Z0-9_.%-]+/graphql": { target: HOST, ...opts },
      // Theme CSS
      "/theme.css": { target: `${HOST}/_SYSTEM_/theme.css`, ...opts },
    },
  },
}));
