import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("should show the alt logo", async ({ page, goto }) => {
  await goto("/catalogue-demo/ssr-catalogue/?logo=UMCGkort.woordbeeld", {
    waitUntil: "hydration",
  });
  await page.getByRole("button", { name: "Accept" }).click();
  await expect(page).toHaveScreenshot({
    clip: { x: 0, y: 0, width: 200, height: 100 },
    threshold: 0.4,
  });
});
