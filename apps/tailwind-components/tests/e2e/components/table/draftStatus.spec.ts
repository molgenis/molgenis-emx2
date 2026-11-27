import { test, expect } from "@playwright/test";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/tailwind-components/#/";

test("the draft status is displayed correctly", async ({ page }) => {
  await page.goto(`${route}table/EMX2.story?schema=pet+store&table=Pet`);

  // find rows by name, not by position, so the test does not depend on the sort order or page
  const search = page.getByRole("searchbox", { name: "Search Pet" });
  const rowWithName = (name: string) =>
    page
      .getByRole("row")
      .filter({ has: page.getByRole("cell", { name, exact: true }) });

  await search.fill("yakul");
  await expect(rowWithName("yakul")).toContainText(
    "Draft yakulherbivorous mammals "
  );

  await search.fill("tweety");
  await expect(rowWithName("tweety")).toBeVisible();
  await expect(rowWithName("tweety")).not.toContainText("Draft");
});
