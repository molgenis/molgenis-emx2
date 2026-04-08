import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test("Can hide and show columns", async ({ page }) => {
  await page.goto(`${route}pet%20store/Pet`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Pet");
  await page.getByRole("button", { name: "Columns" }).click();
  await page.locator('label:has-text("name")').click();
  await page.getByRole("button", { name: "Save" }).click();
  await expect(
    page.getByRole("columnheader", { name: "name" })
  ).not.toBeVisible();
  await page.getByRole("button", { name: "Columns" }).click();
  await page.locator('label:has-text("name")').click();
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.getByRole("columnheader", { name: "name" })).toBeVisible();
});

test("Can change order of columns", async ({ page }) => {
  await page.goto(`${route}pet%20store/Pet`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Pet");
  await expect(page.locator('table.table-fixed tr').first().getByRole("columnheader").first()).toHaveText("name");
  await page.getByRole("button", { name: "Columns" }).click();
  await page.locator('label:has-text("name")').dragTo(page.locator('label:has-text("tags")'));
  await page.getByRole("button", { name: "Save" }).click();
  await expect(page.locator('table.table-fixed tr').first().getByRole("columnheader").first()).toHaveText("category");
});
