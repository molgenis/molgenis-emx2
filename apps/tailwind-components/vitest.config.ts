import { defineVitestConfig } from "@nuxt/test-utils/config";

export default defineVitestConfig({
  test: {
    environment: "nuxt",
    include: ["tests/vitest/**/**/*.spec.ts"],
    coverage: {
      include: ["app/components/**/*.vue", "app/composables/**/*.ts", "app/utils/**/*.ts"],
      exclude: ["app/components/global/**/*.vue", "server/**", "tests/**", "app/types/**"],
      reporter: ["text", "lcov"],
    },
  },
});
