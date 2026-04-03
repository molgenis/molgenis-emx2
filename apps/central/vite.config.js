import {defineConfig} from "vite";
import vue from "@vitejs/plugin-vue";
import monacoEditorPlugin from "vite-plugin-monaco-editor";


const opts = {changeOrigin: true, secure: false, logLevel: "debug"};

export default defineConfig({
    plugins: [
        vue(),
        monacoEditorPlugin({
            languages: ["editorWorkerService", "json"],
        }),
    ],
    base: "",
    server: {
        proxy: {
            "/graphql": {target: "http://localhost:8080/api", ...opts},
            "/api": {target: "http://localhost:8080/", ...opts},
            "/theme.css": {target: "http://localhost:8080/apps/central", ...opts},
            "/apps": {target: "http://localhost:8080", ...opts},
            "^/theme.css": {
                target: "http://localhost:8080/apps/central",
                ...opts,
            },
        },
    },
});
