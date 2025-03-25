import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import monacoEditorPlugin from 'vite-plugin-monaco-editor';

export default defineConfig(() => {
  require("dotenv").config({ path: `./.env` });
  
  return {
  plugins: [
    vue(),
    monacoEditorPlugin({
        languages: ["editorWorkerService", "html", "css", "typescript"],
    }),
  ],
  base: "",
  server: {
    proxy: require("../dev-proxy.config"),
  },
}});
