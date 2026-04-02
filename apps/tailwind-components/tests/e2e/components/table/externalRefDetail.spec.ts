import { test, expect } from "@playwright/test";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test("the draft status is displayed correctly", async ({ page }) => {
  await page.goto(
    `${route}table/EMX2.story?schema=catalogue-demo&table=Resources`
  );

  await expect(page.locator("tbody")).toContainText("main_fdp");
  await page.getByRole("button", { name: "hide menu" }).click();
  await page.getByText("Catalogue", { exact: true }).first().click();
  await expect(page.getByText("Catalogue of resources")).toBeVisible();
});
