import { defineConfig } from "vitest/config";
import { defineVitestProject } from "@nuxt/test-utils/config";

const pureSpecs = [
  "tests/vitest/utils/**/*.spec.ts",
  "tests/vitest/scripts/**/*.spec.ts",
];
const pureSpecsExceptThoseNeedingNuxt = [
  "tests/vitest/utils/**/!(*.nuxt).spec.ts",
  "tests/vitest/scripts/**/!(*.nuxt).spec.ts",
];
const specsNeedingNuxt = "**/*.nuxt.spec.ts";

export default defineConfig({
  test: {
    projects: [
      {
        test: {
          name: "node",
          environment: "node",
          include: pureSpecs,
          exclude: [specsNeedingNuxt],
        },
      },
      await defineVitestProject({
        test: {
          name: "nuxt",
          setupFiles: ["./tests/vitest/setup.ts"],
          hookTimeout: 120000,
          include: ["tests/vitest/**/*.spec.ts"],
          exclude: pureSpecsExceptThoseNeedingNuxt,
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
