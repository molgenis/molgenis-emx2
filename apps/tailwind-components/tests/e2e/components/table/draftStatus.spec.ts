import { test, expect } from "@playwright/test";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test("the draft status is displayed correctly", async ({ page }) => {
  await page.goto(`${route}table/EMX2.story?schema=pet+store&table=Pet`);

  // Verify the expected row is present
  await expect(page.getByRole("cell", { name: "yakul" })).toBeVisible();
  // Verify the draft status is on the expected row
  await expect(page.locator(" table > tbody > tr:nth-child(9)")).toContainText(
    "Draft yakulherbivorous mammals "
  );
});
