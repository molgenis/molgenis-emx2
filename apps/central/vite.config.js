import { defineConfig } from 'vite'
import { createVuePlugin as vue } from "vite-plugin-vue2";

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    base: "",
    server: {
        proxy: {
            "/graphql": { target: "http://localhost:8080/api" },
            "/theme.css": { target: "http://localhost:8080/apps/central" },
            "/apps": { target: "http://localhost:8080" },
            "^/theme.css": {
                target: "http://localhost:8080/apps/central",
            },
        },
    }
})