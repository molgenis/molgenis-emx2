import { defineConfig, devices } from '@playwright/test';
import type { ConfigOptions } from '@nuxt/test-utils/playwright'

export default defineConfig<ConfigOptions>({
  testDir: './tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: process.env.CI ? [['list'],
  ['junit', { outputFile: 'results.xml' }]
  ] : 'html',
  /* start a test server in ci */
   ...(process.env.CI ? {
    webServer: {
      command: '../../gradlew -p ../.. run', // Adjust to your actual command
      url: 'http://localhost:8080/', // Ensure it matches your app's startup URL
      timeout: 180 * 1000, // 3 minutes to allow full startup
      reuseExistingServer: true, // If server is already running, don't restart
      stdout: 'pipe', // Capture stdout logs
      stderr: 'pipe', // Capture stderr logs
    }
  } : {}), // If not in CI, don't include `webServer`
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: "http://localhost:3000/", // change to specific http://localhost:*/, preview, etc.

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
    nuxt: {
      // @ts-ignore
      host: process.env.CI ? "http://localhost:8080" : "http://localhost:3000/",
      build: false
    }
  },

  snapshotPathTemplate: '__screenshots__/{testFilePath}/{arg}{ext}',

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      testIgnore: '*/admin!*.spec.ts'
    },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'e2e/.auth/user.json'
      },
      testMatch: '*/admin!*.spec.ts'
    },
  ],

});

