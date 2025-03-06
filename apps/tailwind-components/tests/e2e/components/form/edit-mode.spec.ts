import { test, expect } from "@playwright/test";

import playwrightConfig from "~/playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}Form.story?schema=pet+store&table=Pet&rowIndex=1`);
  await page.getByText("Jump to", { exact: true }).click({ delay: 300 });
});

test("the form should show the row data", async ({ page }) => {
  await expect(
    page.getByRole("textbox", { name: "name Required" })
  ).toHaveValue("pooky");
  await expect(page.getByRole("button", { name: "cat" })).toBeVisible();
});
