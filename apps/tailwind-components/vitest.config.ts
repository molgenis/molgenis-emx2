import { defineVitestConfig } from "@nuxt/test-utils/config";

export default defineVitestConfig({
  test: {
    setupFiles: ["./tests/vitest/setup.ts"],
    hookTimeout: 20000,
    environment: "nuxt",
    hookTimeout: 30000,
    include: ["tests/vitest/**/**/*.spec.ts"],
    coverage: {
      include: [
        "app/components/**/*.vue",
        "app/composables/**/*.ts",
        "app/utils/**/*.ts",
      ],
      exclude: [
        "app/components/global/**/*.vue",
        "server/**",
        "tests/**",
        "app/types/**",
      ],
      reporter: ["text", "lcov"],
    },
  },
});
