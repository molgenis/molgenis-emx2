import { defineVitestConfig } from "@nuxt/test-utils/config";

export default defineVitestConfig({
  test: {
    environment: "happy-dom",
    include: ["tests/vitest/**/**/*.spec.ts"],
    coverage: {
      include: ["components/**/*.vue", "composables/**/*.ts", "utils/**/*.ts"],
      exclude: ["components/global/**/*.vue"],
      reporter: ["text", "lcov"],
    },
  },
});
