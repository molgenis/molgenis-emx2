import { defineVitestConfig } from '@nuxt/test-utils/config'

export default defineVitestConfig({
  test: {
    environment: "happy-dom",
    include: ["tests/vitest/**/*.spec.ts"],
  }
})