import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("navigate-to-next-page-on-cohorts-list-page", async ({ page }) => {
  await page.goto("/catalogue-demo/ssr-catalogue/ATHLETE");
  await page.getByRole("button", { name: "Accept" }).click();
  await page.getByRole("button", { name: "Cohort studies" }).click();
  await page.locator("a").filter({ hasText: "Go to page 2" }).click();
  await expect(page.getByRole("main")).toContainText("SEPAGES");
});
