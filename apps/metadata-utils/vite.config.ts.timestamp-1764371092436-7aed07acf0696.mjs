// vite.config.ts
import { defineConfig } from "file:///Users/umcg-mswertz/git/molgenis-emx2-bugfixing/molgenis-emx2/apps/node_modules/.pnpm/vite@5.4.21_@types+node@24.10.1_sass@1.70.0_terser@5.44.1/node_modules/vite/dist/node/index.js";
import { resolve } from "path";
import dts from "file:///Users/umcg-mswertz/git/molgenis-emx2-bugfixing/molgenis-emx2/apps/node_modules/.pnpm/vite-plugin-dts@3.6.4_@types+node@24.10.1_rollup@4.53.3_typescript@5.8.2_vite@5.4.21_@t_6d6b35ceca48f3952730538ec4d56965/node_modules/vite-plugin-dts/dist/index.mjs";
var __vite_injected_original_dirname = "/Users/umcg-mswertz/git/molgenis-emx2-bugfixing/molgenis-emx2/apps/metadata-utils";
var vite_config_default = defineConfig({
  plugins: [
    dts({
      insertTypesEntry: true
    })
  ],
  build: {
    lib: {
      entry: resolve(__vite_injected_original_dirname, "src/index.ts"),
      name: "MetaDataUtils",
      fileName: "metadata-utils",
      formats: ["es", "umd", "iife", "cjs"]
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvVXNlcnMvdW1jZy1tc3dlcnR6L2dpdC9tb2xnZW5pcy1lbXgyLWJ1Z2ZpeGluZy9tb2xnZW5pcy1lbXgyL2FwcHMvbWV0YWRhdGEtdXRpbHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9Vc2Vycy91bWNnLW1zd2VydHovZ2l0L21vbGdlbmlzLWVteDItYnVnZml4aW5nL21vbGdlbmlzLWVteDIvYXBwcy9tZXRhZGF0YS11dGlscy92aXRlLmNvbmZpZy50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vVXNlcnMvdW1jZy1tc3dlcnR6L2dpdC9tb2xnZW5pcy1lbXgyLWJ1Z2ZpeGluZy9tb2xnZW5pcy1lbXgyL2FwcHMvbWV0YWRhdGEtdXRpbHMvdml0ZS5jb25maWcudHNcIjtpbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tIFwidml0ZVwiO1xuaW1wb3J0IHsgcmVzb2x2ZSB9IGZyb20gXCJwYXRoXCI7XG5pbXBvcnQgZHRzIGZyb20gXCJ2aXRlLXBsdWdpbi1kdHNcIjtcblxuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKHtcbiAgcGx1Z2luczogW1xuICAgIGR0cyh7XG4gICAgICBpbnNlcnRUeXBlc0VudHJ5OiB0cnVlLFxuICAgIH0pLFxuICBdLFxuICBidWlsZDoge1xuICAgIGxpYjoge1xuICAgICAgZW50cnk6IHJlc29sdmUoX19kaXJuYW1lLCBcInNyYy9pbmRleC50c1wiKSxcbiAgICAgIG5hbWU6IFwiTWV0YURhdGFVdGlsc1wiLFxuICAgICAgZmlsZU5hbWU6IFwibWV0YWRhdGEtdXRpbHNcIixcbiAgICAgIGZvcm1hdHM6IFtcImVzXCIsIFwidW1kXCIsIFwiaWlmZVwiLCBcImNqc1wiXSxcbiAgICB9LFxuICB9LFxufSk7XG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQXFhLFNBQVMsb0JBQW9CO0FBQ2xjLFNBQVMsZUFBZTtBQUN4QixPQUFPLFNBQVM7QUFGaEIsSUFBTSxtQ0FBbUM7QUFJekMsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUztBQUFBLElBQ1AsSUFBSTtBQUFBLE1BQ0Ysa0JBQWtCO0FBQUEsSUFDcEIsQ0FBQztBQUFBLEVBQ0g7QUFBQSxFQUNBLE9BQU87QUFBQSxJQUNMLEtBQUs7QUFBQSxNQUNILE9BQU8sUUFBUSxrQ0FBVyxjQUFjO0FBQUEsTUFDeEMsTUFBTTtBQUFBLE1BQ04sVUFBVTtBQUFBLE1BQ1YsU0FBUyxDQUFDLE1BQU0sT0FBTyxRQUFRLEtBQUs7QUFBQSxJQUN0QztBQUFBLEVBQ0Y7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
