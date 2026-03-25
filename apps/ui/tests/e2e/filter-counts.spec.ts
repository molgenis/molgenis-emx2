import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.describe("Filter counts", () => {
  test("filter options show counts in parentheses", async ({ page }) => {
    await page.goto(`${route}pet%20store/Pet`);
    await page.waitForLoadState("networkidle");

    const sidebar = page.locator(".filter-sidebar-context");
    await expect(sidebar).toBeVisible();

    const countPattern = sidebar.getByText(/\(\d+\)/);
    await expect(countPattern.first()).toBeVisible({ timeout: 10000 });
  });

  test("clicking a filter option filters the table", async ({ page }) => {
    await page.goto(`${route}pet%20store/Pet`);
    await page.waitForLoadState("networkidle");

    const table = page.locator("table");
    await expect(table).toBeVisible();
    const rowsBefore = await table.locator("tbody tr").count();

    const sidebar = page.locator(".filter-sidebar-context");
    const filterCheckbox = sidebar.locator("label").filter({
      has: page.getByText(/\(\d+\)/),
    }).first();
    await expect(filterCheckbox).toBeVisible({ timeout: 10000 });
    await filterCheckbox.click();
    await page.waitForLoadState("networkidle");

    const rowsAfter = await table.locator("tbody tr").count();
    expect(rowsAfter).toBeGreaterThan(0);
    expect(rowsAfter).toBeLessThanOrEqual(rowsBefore);
  });

  test("filter via URL shows results and counts", async ({ page }) => {
    await page.goto(`${route}pet%20store/Pet?category.name=cat`);
    await page.waitForLoadState("networkidle");

    const table = page.locator("table");
    await expect(table).toBeVisible();
    const rowCount = await table.locator("tbody tr").count();
    expect(rowCount).toBeGreaterThan(0);

    const sidebar = page.locator(".filter-sidebar-context");
    const countPattern = sidebar.getByText(/\(\d+\)/);
    await expect(countPattern.first()).toBeVisible({ timeout: 10000 });
  });

  test("ontology parent term matches children via URL", async ({ page }) => {
    await page.goto(`${route}pet%20store/Pet?tags.name=colors`);
    await page.waitForLoadState("networkidle");

    const table = page.locator("table");
    await expect(table).toBeVisible();
    const rowCount = await table.locator("tbody tr").count();
    expect(rowCount).toBeGreaterThan(0);
  });
});
