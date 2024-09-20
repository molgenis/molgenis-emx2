import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("show network of networks", async ({ page }) => {
  await page.goto("/catalogue-demo/ssr-catalogue/testNetworkofNetworks");
  await page.getByRole("button", { name: "Accept" }).click();
  await expect(page.getByText("7", { exact: true })).toBeVisible();
  await expect(page.getByText("4", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Cohort studies" }).click();
  await expect(page.getByText("4 cohort studies")).toBeVisible();
  await page.goto("/catalogue-demo/ssr-catalogue/testNetworkofNetworks");
  await page.getByRole("button", { name: "Variables" }).click();
  await expect(page.getByText("7 variables")).toBeVisible();
  //todo check if not should be 9
  await page.getByRole("heading", { name: "Resources" }).click();
  await page.getByRole("heading", { name: "Resources" }).click();
  await expect(page.getByText("testCohort4")).toBeVisible();
  await page.getByRole("button", { name: "Harmonisations" }).click();
  await expect(
    page.getByRole("cell", { name: "testCohort4" }).locator("span")
  ).toBeVisible();
});
