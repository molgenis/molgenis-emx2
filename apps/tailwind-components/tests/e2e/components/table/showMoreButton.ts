import { test, expect } from "@playwright/test";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test("Clicking the show more button displays all values and allows click through", async ({
  page,
}) => {
  await page.goto(`${route}table/EMX2.story?schema=pet+store&table=Pet`);

  await page.getByRole("button", { name: "More" }).first().click();
  await page.getByLabel("orders").getByText("spike").click();
  await expect(page.getByRole("cell", { name: "spike" })).toBeVisible();
});
