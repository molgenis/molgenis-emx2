import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.describe("filter sidebar", () => {
  test("sidebar renders with filters heading", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const filtersHeading = page.getByRole("heading", { level: 2 }).filter({
      hasText: "Filters",
    });
    await expect(filtersHeading).toBeVisible();
  });

  test("filter sections expand and collapse", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const filterSections = page.locator('[role="button"][aria-expanded]');
    const sectionCount = await filterSections.count();
    expect(sectionCount).toBeGreaterThanOrEqual(1);

    const firstSection = filterSections.first();
    await expect(firstSection).toBeVisible();

    const isExpandedInitially =
      (await firstSection.getAttribute("aria-expanded")) === "true";
    expect(isExpandedInitially).toBe(true);

    await firstSection.click();
    const isCollapsedAfterClick =
      (await firstSection.getAttribute("aria-expanded")) === "false";
    expect(isCollapsedAfterClick).toBe(true);

    await firstSection.click();
    const isExpandedAfterReclick =
      (await firstSection.getAttribute("aria-expanded")) === "true";
    expect(isExpandedAfterReclick).toBe(true);
  });

  test("filter sections contain checkboxes", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const labels = page.locator('label:has(input[type="checkbox"])');
    const labelCount = await labels.count();
    expect(labelCount).toBeGreaterThanOrEqual(1);
  });

  test("table renders with data", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const table = page.locator("table");
    await expect(table).toBeVisible();

    const rows = page.locator("table tbody tr");
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThanOrEqual(1);
  });

  test("customize button exists and opens modal", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const customizeButton = page.getByRole("button", {
      name: "Customize",
    });
    await expect(customizeButton).toBeVisible();

    await customizeButton.click();
    await page.waitForTimeout(1000);

    const modalDialog = page.locator('[role="dialog"]');
    await expect(modalDialog).toBeVisible();

    const cancelButton = page.getByRole("button", {
      name: /cancel/i,
    });
    await cancelButton.click();

    await expect(modalDialog).not.toBeVisible();
  });

  test("search filter input works", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const searchInput = page.locator('input[placeholder="Search..."]');
    await expect(searchInput).toBeVisible();

    await searchInput.fill("test");
    const value = await searchInput.inputValue();
    expect(value).toBe("test");

    await searchInput.clear();
    const clearedValue = await searchInput.inputValue();
    expect(clearedValue).toBe("");
  });

  test("filter interaction updates URL", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const firstLabel = page
      .locator('label:has(input[type="checkbox"])')
      .first();
    await expect(firstLabel).toBeVisible();

    const initialUrl = page.url();
    await firstLabel.click();
    await page.waitForTimeout(500);

    const updatedUrl = page.url();
    expect(updatedUrl).not.toBe(initialUrl);

    await firstLabel.click();
    await page.waitForTimeout(500);
  });
});
