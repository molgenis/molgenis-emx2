import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import monacoEditorPlugin from 'vite-plugin-monaco-editor';

export default defineConfig((command) => {
  require("dotenv").config({ path: `./.env` });
  
  return {
  plugins: [
    vue(),
    monacoEditorPlugin({
        languages: ["editorWorkerService", "html", "css", "typescript"],
    }),
  ],
  base: command === "serve" ? "/" :"apps/pages/",
  server: {
    proxy: require("../dev-proxy.config"),
  },
}});
