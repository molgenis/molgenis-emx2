import { defineConfig, devices, PlaywrightTestConfig } from "@playwright/test";

export default defineConfig<PlaywrightTestConfig>({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: process.env.CI
    ? [["list"], ["junit", { outputFile: "results.xml" }]]
    : "html",
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:5173/", // change to specific http://localhost:*/, preview, etc.
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
