import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { createHtmlPlugin } from "vite-plugin-html";
import monacoEditorPlugin from "vite-plugin-monaco-editor";

const HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org/";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "directory-demo";

const opts = { changeOrigin: true, secure: false, logLevel: "debug" };

// https://vitejs.dev/config/
export default defineConfig(({ command }) => ({
  base: command === "serve" ? "/" : "apps/directory/",
  build: { sourcemap: true },
  plugins: [
    vue(),
    createHtmlPlugin({
      entry: "src/main.ts",
      template: command === "serve" ? "dev-index.html" : "index.html",
    }),
    monacoEditorPlugin({
      languages: ["editorWorkerService", "json"],
    }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      "/graphql": {
        target: `${HOST}/${SCHEMA}`,
        ...opts,
      },
      /* should match only '/schema_name/graphql', previous ** was to eager also matching if graphql was /graphql or /a/b/graphql */
      "^/[a-zA-Z0-9_.-]+/graphql": {
        target: `${HOST}`,
        ...opts,
      },
      "/api": { target: `${HOST}`, ...opts },
      "/apps": { target: `${HOST}`, ...opts },
      "/theme.css": { target: `${HOST}/${SCHEMA}`, ...opts },
      "/public_html/apps/directory/img/": {
        secure: false,
        rewrite: (path) =>
          path.replace(
            /^\/public_html\/apps\/directory/,
            ""
          ) /** removes the part of the url, so this will go straight to /img in public folder */,
        target: "http://localhost:5173",
      },
    },
  },
}));
