import { defineVitestConfig } from "@nuxt/test-utils/config";

export default defineVitestConfig({
  test: {
    setupFiles: ["../tailwind-components/tests/vitest/setup.ts"],
    hookTimeout: 20000,
    environment: "nuxt",
    include: ["tests/**/**/*.test.ts"],
    coverage: {
      include: [
        "app/components/**/*.vue",
        "app/composables/**/*.ts",
        "app/utils/**/*.ts",
      ],
      exclude: ["tests/**", "app/interfaces/**"],
      reporter: ["text", "lcov"],
    },
  },
});
