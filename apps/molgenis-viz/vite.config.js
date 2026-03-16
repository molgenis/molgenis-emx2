import { defineConfig } from "vite";
import path from "path";
import vue from "@vitejs/plugin-vue";
import dotenv from "dotenv";
import devProxy from "./dev-proxy.config";
import { fileURLToPath } from "url";

// needed because __dirname is not available in ESM
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// load env
dotenv.config({ path: "./.env" });

// basic build conf for both library and app
const conf = {
  plugins: [vue()],
  base: "apps/molgenis-viz/",
  resolve: {
    alias: {
      vue: "vue/dist/vue.runtime.esm-bundler.js",
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'legacy',
        additionalData: `
          @import "@/styles/heightwidth.scss";
          @import "@/styles/mixins.scss";
          @import "@/styles/padding.scss";
          @import "@/styles/palettes.scss";
          @import "@/styles/resets.scss";
          @import "@/styles/textPosition.scss";
          @import "@/styles/variables.scss";
        `,
      },
    },
  },
};

export default defineConfig(({ command, mode }) => {
  if (command === "serve") {
    return {
      ...conf,
      base: "/",
      server: {
        proxy: devProxy,
      },
    };
  }

  if (command === "build" && mode === "lib") {
    return {
      ...conf,
      build: {
        lib: {
          entry: path.resolve(__dirname, "lib/main.js"),
          name: "molgenis-viz",
          fileName: (format) => `molgenis-viz.${format}.js`,
        },
        rollupOptions: {
          external: ["vue"],
          output: {
            globals: {
              vue: "Vue",
            },
          },
        },
      },
    };
  }

  if (command === "build" && mode === "app") {
    return {
      ...conf,
      build: {
        rollupOptions: {
          output: {
            assetFileNames: (assetInfo) => {
              const extension = assetInfo.name.split(".").pop();
              if (/png|jpg|svg/.test(extension)) {
                return `img/[name]-[hash][extname]`;
              }
              return `assets/[name]-[hash][extname]`;
            },
          },
        },
      },
    };
  }
});