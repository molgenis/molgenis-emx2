var __getOwnPropNames = Object.getOwnPropertyNames;
var __commonJS = (cb, mod) => function __require() {
  return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
};

// ../dev-proxy.config.js
var require_dev_proxy_config = __commonJS({
  "../dev-proxy.config.js"(exports, module) {
    var HOST = process.env.MOLGENIS_APPS_HOST || "https://emx2.dev.molgenis.org";
    var SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "pet store";
    var opts = { changeOrigin: true, secure: false, logLevel: "debug" };
    module.exports = {
      "/api/graphql": {
        target: `${HOST}/${SCHEMA}`,
        ...opts
      },
      "^/[a-zA-Z0-9_.%-]+/api/graphql": {
        target: HOST,
        ...opts
      },
      "^/[a-zA-Z0-9_.%-]+/graphql": {
        target: HOST,
        ...opts
      },
      "^/[a-zA-Z0-9_.%-]+/api/reports": {
        target: HOST,
        ...opts
      },
      "^/[a-zA-Z0-9_.%-]+/api/file": {
        target: HOST,
        ...opts
      },
      "^/[a-zA-Z0-9_.%-]+/api/trigger": {
        target: HOST,
        ...opts
      },
      "/api": {
        target: `${HOST}/api`,
        ...opts
      },
      "/graphql": {
        target: `${HOST}/${SCHEMA}/graphql`,
        ...opts
      },
      "/reports": {
        target: `${HOST}/${SCHEMA}/api/reports`,
        ...opts
      },
      "/apps": {
        target: HOST,
        ...opts
      },
      "/theme.css": {
        target: `${HOST}/${SCHEMA}/theme.css`,
        ...opts
      }
    };
  }
});

// vite.config.ts
import { defineConfig } from "file:///Users/umcg-mswertz/git/molgenis-emx2-bugfixing/molgenis-emx2/apps/node_modules/.pnpm/vite@5.4.21_@types+node@24.10.1_sass@1.70.0_terser@5.44.1/node_modules/vite/dist/node/index.js";
import path from "path";
import dts from "file:///Users/umcg-mswertz/git/molgenis-emx2-bugfixing/molgenis-emx2/apps/node_modules/.pnpm/vite-plugin-dts@3.6.4_@types+node@24.10.1_rollup@4.53.3_typescript@5.6.2_vite@5.4.21_@t_5338d65707b44f5909a89dd68c93d825/node_modules/vite-plugin-dts/dist/index.mjs";
var __vite_injected_original_dirname = "/Users/umcg-mswertz/git/molgenis-emx2-bugfixing/molgenis-emx2/apps/emx2-analytics";
var vite_config_default = defineConfig({
  plugins: [dts()],
  server: {
    proxy: require_dev_proxy_config()
  },
  build: {
    lib: {
      entry: path.resolve(__vite_injected_original_dirname, "src/lib/analytics.ts"),
      name: "analytics",
      fileName: (format) => `analytics.${format}.js`
    },
    sourcemap: true,
    emptyOutDir: true
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsiLi4vZGV2LXByb3h5LmNvbmZpZy5qcyIsICJ2aXRlLmNvbmZpZy50cyJdLAogICJzb3VyY2VzQ29udGVudCI6IFsiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9Vc2Vycy91bWNnLW1zd2VydHovZ2l0L21vbGdlbmlzLWVteDItYnVnZml4aW5nL21vbGdlbmlzLWVteDIvYXBwc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL1VzZXJzL3VtY2ctbXN3ZXJ0ei9naXQvbW9sZ2VuaXMtZW14Mi1idWdmaXhpbmcvbW9sZ2VuaXMtZW14Mi9hcHBzL2Rldi1wcm94eS5jb25maWcuanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL1VzZXJzL3VtY2ctbXN3ZXJ0ei9naXQvbW9sZ2VuaXMtZW14Mi1idWdmaXhpbmcvbW9sZ2VuaXMtZW14Mi9hcHBzL2Rldi1wcm94eS5jb25maWcuanNcIjtjb25zdCBIT1NUID0gcHJvY2Vzcy5lbnYuTU9MR0VOSVNfQVBQU19IT1NUIHx8IFwiaHR0cHM6Ly9lbXgyLmRldi5tb2xnZW5pcy5vcmdcIjtcbmNvbnN0IFNDSEVNQSA9IHByb2Nlc3MuZW52Lk1PTEdFTklTX0FQUFNfU0NIRU1BIHx8IFwicGV0IHN0b3JlXCI7XG5cbmNvbnN0IG9wdHMgPSB7IGNoYW5nZU9yaWdpbjogdHJ1ZSwgc2VjdXJlOiBmYWxzZSwgbG9nTGV2ZWw6IFwiZGVidWdcIiB9O1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgXCIvYXBpL2dyYXBocWxcIjoge1xuICAgIHRhcmdldDogYCR7SE9TVH0vJHtTQ0hFTUF9YCxcbiAgICAuLi5vcHRzLFxuICB9LFxuICBcIl4vW2EtekEtWjAtOV8uJS1dKy9hcGkvZ3JhcGhxbFwiOiB7XG4gICAgdGFyZ2V0OiBIT1NULFxuICAgIC4uLm9wdHMsXG4gIH0sXG4gIFwiXi9bYS16QS1aMC05Xy4lLV0rL2dyYXBocWxcIjoge1xuICAgIHRhcmdldDogSE9TVCxcbiAgICAuLi5vcHRzLFxuICB9LFxuICBcIl4vW2EtekEtWjAtOV8uJS1dKy9hcGkvcmVwb3J0c1wiOiB7XG4gICAgdGFyZ2V0OiBIT1NULFxuICAgIC4uLm9wdHMsXG4gIH0sXG4gIFwiXi9bYS16QS1aMC05Xy4lLV0rL2FwaS9maWxlXCI6IHtcbiAgICB0YXJnZXQ6IEhPU1QsXG4gICAgLi4ub3B0cyxcbiAgfSxcbiAgXCJeL1thLXpBLVowLTlfLiUtXSsvYXBpL3RyaWdnZXJcIjoge1xuICAgIHRhcmdldDogSE9TVCxcbiAgICAuLi5vcHRzLFxuICB9LFxuICBcIi9hcGlcIjoge1xuICAgIHRhcmdldDogYCR7SE9TVH0vYXBpYCxcbiAgICAuLi5vcHRzLFxuICB9LFxuICBcIi9ncmFwaHFsXCI6IHtcbiAgICB0YXJnZXQ6IGAke0hPU1R9LyR7U0NIRU1BfS9ncmFwaHFsYCxcbiAgICAuLi5vcHRzLFxuICB9LFxuICBcIi9yZXBvcnRzXCI6IHtcbiAgICB0YXJnZXQ6IGAke0hPU1R9LyR7U0NIRU1BfS9hcGkvcmVwb3J0c2AsXG4gICAgLi4ub3B0cyxcbiAgfSxcbiAgXCIvYXBwc1wiOiB7XG4gICAgdGFyZ2V0OiBIT1NULFxuICAgIC4uLm9wdHMsXG4gIH0sXG4gIFwiL3RoZW1lLmNzc1wiOiB7XG4gICAgdGFyZ2V0OiBgJHtIT1NUfS8ke1NDSEVNQX0vdGhlbWUuY3NzYCxcbiAgICAuLi5vcHRzLFxuICB9LFxufTtcbiIsICJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiL1VzZXJzL3VtY2ctbXN3ZXJ0ei9naXQvbW9sZ2VuaXMtZW14Mi1idWdmaXhpbmcvbW9sZ2VuaXMtZW14Mi9hcHBzL2VteDItYW5hbHl0aWNzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCIvVXNlcnMvdW1jZy1tc3dlcnR6L2dpdC9tb2xnZW5pcy1lbXgyLWJ1Z2ZpeGluZy9tb2xnZW5pcy1lbXgyL2FwcHMvZW14Mi1hbmFseXRpY3Mvdml0ZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL1VzZXJzL3VtY2ctbXN3ZXJ0ei9naXQvbW9sZ2VuaXMtZW14Mi1idWdmaXhpbmcvbW9sZ2VuaXMtZW14Mi9hcHBzL2VteDItYW5hbHl0aWNzL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcbmltcG9ydCB2dWUgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlJ1xuaW1wb3J0IHBhdGggZnJvbSAncGF0aCc7IC8vIEltcG9ydCB0aGUgJ3BhdGgnIG1vZHVsZVxuaW1wb3J0IGR0cyBmcm9tICd2aXRlLXBsdWdpbi1kdHMnXG5cbi8vIGh0dHBzOi8vdml0ZWpzLmRldi9jb25maWcvXG5leHBvcnQgZGVmYXVsdCBkZWZpbmVDb25maWcoe1xuICBwbHVnaW5zOiBbZHRzKCldLFxuICBzZXJ2ZXI6IHtcbiAgICBwcm94eTogcmVxdWlyZShcIi4uL2Rldi1wcm94eS5jb25maWdcIiksXG4gIH0sXG4gIGJ1aWxkOiB7XG4gICAgbGliOiB7XG4gICAgICBlbnRyeTogcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgXCJzcmMvbGliL2FuYWx5dGljcy50c1wiKSwgXG4gICAgICBuYW1lOiBcImFuYWx5dGljc1wiLFxuICAgICAgZmlsZU5hbWU6IChmb3JtYXQpID0+IGBhbmFseXRpY3MuJHtmb3JtYXR9LmpzYCxcbiAgICB9LFxuICAgIHNvdXJjZW1hcDogdHJ1ZSxcbiAgICBlbXB0eU91dERpcjogdHJ1ZSxcbiAgfSxcbn0pO1xuIl0sCiAgIm1hcHBpbmdzIjogIjs7Ozs7O0FBQUE7QUFBQTtBQUFrWSxRQUFNLE9BQU8sUUFBUSxJQUFJLHNCQUFzQjtBQUNqYixRQUFNLFNBQVMsUUFBUSxJQUFJLHdCQUF3QjtBQUVuRCxRQUFNLE9BQU8sRUFBRSxjQUFjLE1BQU0sUUFBUSxPQUFPLFVBQVUsUUFBUTtBQUVwRSxXQUFPLFVBQVU7QUFBQSxNQUNmLGdCQUFnQjtBQUFBLFFBQ2QsUUFBUSxHQUFHLElBQUksSUFBSSxNQUFNO0FBQUEsUUFDekIsR0FBRztBQUFBLE1BQ0w7QUFBQSxNQUNBLGtDQUFrQztBQUFBLFFBQ2hDLFFBQVE7QUFBQSxRQUNSLEdBQUc7QUFBQSxNQUNMO0FBQUEsTUFDQSw4QkFBOEI7QUFBQSxRQUM1QixRQUFRO0FBQUEsUUFDUixHQUFHO0FBQUEsTUFDTDtBQUFBLE1BQ0Esa0NBQWtDO0FBQUEsUUFDaEMsUUFBUTtBQUFBLFFBQ1IsR0FBRztBQUFBLE1BQ0w7QUFBQSxNQUNBLCtCQUErQjtBQUFBLFFBQzdCLFFBQVE7QUFBQSxRQUNSLEdBQUc7QUFBQSxNQUNMO0FBQUEsTUFDQSxrQ0FBa0M7QUFBQSxRQUNoQyxRQUFRO0FBQUEsUUFDUixHQUFHO0FBQUEsTUFDTDtBQUFBLE1BQ0EsUUFBUTtBQUFBLFFBQ04sUUFBUSxHQUFHLElBQUk7QUFBQSxRQUNmLEdBQUc7QUFBQSxNQUNMO0FBQUEsTUFDQSxZQUFZO0FBQUEsUUFDVixRQUFRLEdBQUcsSUFBSSxJQUFJLE1BQU07QUFBQSxRQUN6QixHQUFHO0FBQUEsTUFDTDtBQUFBLE1BQ0EsWUFBWTtBQUFBLFFBQ1YsUUFBUSxHQUFHLElBQUksSUFBSSxNQUFNO0FBQUEsUUFDekIsR0FBRztBQUFBLE1BQ0w7QUFBQSxNQUNBLFNBQVM7QUFBQSxRQUNQLFFBQVE7QUFBQSxRQUNSLEdBQUc7QUFBQSxNQUNMO0FBQUEsTUFDQSxjQUFjO0FBQUEsUUFDWixRQUFRLEdBQUcsSUFBSSxJQUFJLE1BQU07QUFBQSxRQUN6QixHQUFHO0FBQUEsTUFDTDtBQUFBLElBQ0Y7QUFBQTtBQUFBOzs7QUNsRHFhLFNBQVMsb0JBQW9CO0FBRWxjLE9BQU8sVUFBVTtBQUNqQixPQUFPLFNBQVM7QUFIaEIsSUFBTSxtQ0FBbUM7QUFNekMsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUyxDQUFDLElBQUksQ0FBQztBQUFBLEVBQ2YsUUFBUTtBQUFBLElBQ04sT0FBTztBQUFBLEVBQ1Q7QUFBQSxFQUNBLE9BQU87QUFBQSxJQUNMLEtBQUs7QUFBQSxNQUNILE9BQU8sS0FBSyxRQUFRLGtDQUFXLHNCQUFzQjtBQUFBLE1BQ3JELE1BQU07QUFBQSxNQUNOLFVBQVUsQ0FBQyxXQUFXLGFBQWEsTUFBTTtBQUFBLElBQzNDO0FBQUEsSUFDQSxXQUFXO0FBQUEsSUFDWCxhQUFhO0FBQUEsRUFDZjtBQUNGLENBQUM7IiwKICAibmFtZXMiOiBbXQp9Cg==
