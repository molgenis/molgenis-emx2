import { defineConfig, devices, PlaywrightTestConfig } from "@playwright/test";
import { e2eBaseUrl } from "../dev-env.js";

export default defineConfig<PlaywrightTestConfig>({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI
    ? [["list"], ["junit", { outputFile: "results.xml" }]]
    : "html",
  use: {
    baseURL: e2eBaseUrl(
      "MOLGENIS_PORT_APP_DIRECTORY",
      "http://localhost:5173/"
    ),
    trace: "on-first-retry",
  },

  snapshotPathTemplate: "__screenshots__/{testFilePath}/{arg}{ext}",

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
