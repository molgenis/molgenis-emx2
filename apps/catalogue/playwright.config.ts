import { defineConfig, devices } from "@playwright/test";
import type { ConfigOptions } from "@nuxt/test-utils/playwright";
import { devPort, e2eBaseUrl } from "../dev-env.js";

const serverPort = devPort("MOLGENIS_PORT_APP_CATALOGUE", 3000);

export default defineConfig<ConfigOptions>({
  webServer: {
    command: "node .output/server/index.mjs",
    url: `http://127.0.0.1:${serverPort}`,
    env: { PORT: String(serverPort) },
    reuseExistingServer: false,
    timeout: 120 * 1000,
  },
  testDir: "./tests",
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* fail faster on CI. */
  maxFailures: process.env.CI ? 1 : 5,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: process.env.CI
    ? [["list"], ["junit", { outputFile: "test-results/results.xml" }]]
    : "html",
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: e2eBaseUrl(
      "MOLGENIS_PORT_APP_CATALOGUE",
      "https://emx2.dev.molgenis.org/"
    ),
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "on-first-retry",
    nuxt: {
      host: e2eBaseUrl("MOLGENIS_PORT_APP_CATALOGUE", "http://localhost:3000/"),
      build: false,
    },
  },

  snapshotPathTemplate: "__screenshots__/{testFilePath}/{arg}{ext}",

  /* Configure projects for major browsers */
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
      testIgnore: "*/admin!*.spec.ts",
    },
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        storageState: "e2e/.auth/user.json",
      },
      testMatch: "*/admin!*.spec.ts",
    },
  ],
});
