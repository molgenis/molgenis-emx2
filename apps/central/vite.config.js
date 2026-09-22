import {defineConfig} from "vite";
import vue from "@vitejs/plugin-vue";
import monacoEditorPlugin from "vite-plugin-monaco-editor";
import {appsHost} from "../dev-env";


const HOST = appsHost("http://localhost:8080");

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
            "/graphql": {target: `${HOST}/api`, ...opts},
            "/api": {target: `${HOST}/`, ...opts},
            "/theme.css": {target: `${HOST}/apps/central`, ...opts},
            "/apps": {target: HOST, ...opts},
            "^/theme.css": {
                target: `${HOST}/apps/central`,
                ...opts,
            },
        },
    },
});
