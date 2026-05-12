import { expect, test } from "@nuxt/test-utils/playwright";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.use({ storageState: "playwright/.auth/user.json" });

test.beforeEach(async ({ page, goto }) => {
  await goto(`${route}table/EMX2.story?schema=pet+store&table=Category`, {
    waitUntil: "hydration",
  });
  await expect(page.getByText("TableEMX2").first()).toBeVisible();
  await expect(page.getByLabel("Schema:")).toHaveValue("pet store");
});

test("the row should be removed from the table after deletion", async ({
  page,
  goto,
}) => {
  await page.getByRole("checkbox", { name: "Is Editable:" }).check();
  await expect(
    page.getByRole("button", { name: "Add Category" })
  ).toBeVisible();

  // create row to delete
  await page.getByRole("button", { name: "Add Category" }).click();
  await page.getByRole("textbox", { name: "name Required" }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill("deltest");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByRole("button", { name: "Cancel" }).click();

  // delete row

  await page.getByRole("searchbox", { name: "Search Category" }).click();
  await page
    .getByRole("searchbox", { name: "Search Category" })
    .fill("deltest");
  await page.getByText("deltest", { exact: true }).click();
  // header is in row 0, so row with deltest is row 1
  await page.getByRole("row").nth(1).hover();
  await page.getByRole("button", { name: 'delete{"name":"deltest"}' }).click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await expect(page.getByRole("row")).toHaveCount(1);
});
