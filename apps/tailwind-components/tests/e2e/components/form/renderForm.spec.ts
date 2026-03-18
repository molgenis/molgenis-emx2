import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }, testInfo) => {
  await page.goto(`${route}Form.story?schema=catalogue-demo&table=Collections`);
  testInfo.setTimeout(testInfo.timeout + 30_000);
});

test("it should render the form", async ({ page }) => {
  await expect(page.getByRole("main")).toContainText("id");
  await expect(page.getByRole("main")).toContainText("pid");
  await expect(page.getByRole("main")).toContainText("Name");
  await expect(page.getByLabel("id Required", { exact: true })).toBeVisible();
});
