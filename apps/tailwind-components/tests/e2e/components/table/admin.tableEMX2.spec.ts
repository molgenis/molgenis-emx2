import { test, expect } from "@playwright/test";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.use({ storageState: "playwright/.auth/user.json" });

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}table/EMX2.story?schema=pet+store&table=Category`);
  await expect(page.getByText("TableEMX2").first()).toBeVisible();
  await expect(page.getByLabel("Schema:")).toHaveValue("pet store");
  await expect(
    page
      .locator("div")
      .filter({ hasText: /^Table: CategoryOrderPetTagUser$/ })
      .locator("#table-select")
  ).toHaveValue("Category");
});

test("the row should be removed from the table after deletion", async ({
  page,
}) => {
  await page.getByRole("link", { name: "TableEMX2" }).click();
  await page.getByLabel("Is Editable:").check();
  await page.getByRole("checkbox", { name: "Is Editable:" }).check();
  // add
  await expect(
    page.getByRole("button", { name: "Add Category" })
  ).toBeVisible();
  await page.getByRole("button", { name: "Add Category" }).click();
  await expect(
    page.getByRole("button", { name: "Save", exact: true })
  ).toBeVisible();
  await page.getByRole("textbox", { name: "name Required" }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill("testdel");
  await page.getByRole("button", { name: "Save", exact: true }).click();

  // delete
  await expect(
    page.getByRole("cell", { name: "testdel", exact: true })
  ).toBeVisible();

  await page
    .getByText("deletetestdel", { exact: true })
    .filter({ visible: false })
    .dispatchEvent("click");

  await expect(
    page.getByRole("heading", { name: "Delete Category" })
  ).toBeVisible();
  await expect(page.getByRole("button", { name: "Delete" })).toBeVisible();
  await page.getByRole("button", { name: "Delete" }).click();

  await page.waitForTimeout(1000); // wait for the table to update

  // check
  await expect(
    page.getByRole("cell", { name: "testdel", exact: true })
  ).not.toBeVisible();
});
