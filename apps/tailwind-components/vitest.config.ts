import { defineConfig } from "vitest/config";
import { defineVitestProject } from "@nuxt/test-utils/config";

const pureTests = [
  "tests/vitest/utils/**/*.test.ts",
  "tests/vitest/scripts/**/*.test.ts",
];
const pureTestsExceptThoseNeedingNuxt = [
  "tests/vitest/utils/**/!(*.nuxt).test.ts",
  "tests/vitest/scripts/**/!(*.nuxt).test.ts",
];
const testsNeedingNuxt = "**/*.nuxt.test.ts";

export default defineConfig({
  test: {
    projects: [
      {
        test: {
          name: "node",
          environment: "node",
          include: pureTests,
          exclude: [testsNeedingNuxt],
        },
      },
      await defineVitestProject({
        test: {
          name: "nuxt",
          setupFiles: ["./tests/vitest/setup.ts"],
          hookTimeout: 120000,
          include: ["tests/vitest/**/*.test.ts"],
          exclude: pureTestsExceptThoseNeedingNuxt,
        },
      }),
    ],
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
