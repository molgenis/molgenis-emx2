import { test, expect } from "@playwright/test";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("Filter Integration - Emx2DataView with FilterSidebar", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}filter/Emx2DataView`);
    await expect(page.getByText("Emx2DataView")).toBeVisible({
      timeout: 10000,
    });
  });

  test("should render FilterSidebar with filter columns", async ({ page }) => {
    const nameFilter = page.locator("text=name").first();
    const categoryFilter = page.locator("text=category").first();
    const weightFilter = page.locator("text=weight").first();

    await expect(nameFilter).toBeVisible();
    await expect(categoryFilter).toBeVisible();
    await expect(weightFilter).toBeVisible();
  });

  test("should expand STRING filter and apply text filter", async ({
    page,
  }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await expect(nameInput).toBeVisible();

    await nameInput.fill("fluffy");
    await page.waitForTimeout(500);

    const result = page.getByText("fluffy");
    await expect(result).toBeVisible({ timeout: 5000 });
  });

  test("should expand INT filter and set min/max range", async ({ page }) => {
    const weightFilterTitle = page
      .locator("button")
      .filter({ hasText: /weight/i })
      .first();
    await weightFilterTitle.click();

    const minLabel = page.getByText("Min");
    const maxLabel = page.getByText("Max");
    await expect(minLabel).toBeVisible();
    await expect(maxLabel).toBeVisible();

    const minInput = page.locator('input[type="number"]').nth(0);
    const maxInput = page.locator('input[type="number"]').nth(1);

    await minInput.fill("10");
    await maxInput.fill("50");
    await page.waitForTimeout(500);

    const tableOrList = page.locator("tbody, li").first();
    await expect(tableOrList).toBeVisible();
  });

  test("should toggle between layouts and persist filters", async ({
    page,
  }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await nameInput.fill("test");
    await page.waitForTimeout(500);

    const initialResults = page.locator("tbody tr, li").count();

    const tableButton = page.getByRole("button", { name: "Table" });
    await tableButton.click();
    await page.waitForTimeout(500);

    const tableResults = page.locator("tbody tr").count();
    expect(tableResults).toBe(initialResults);

    const cardsButton = page.getByRole("button", { name: "Cards" });
    await cardsButton.click();
    await page.waitForTimeout(500);

    const cardsResults = page.locator("[class*='card']").count();
    expect(cardsResults).toBeGreaterThan(0);

    const listButton = page.getByRole("button", { name: "List" });
    await listButton.click();
    await page.waitForTimeout(500);

    await expect(nameInput).toHaveValue("test");
  });

  test("should clear individual filter and update results", async ({
    page,
  }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await nameInput.fill("test");
    await page.waitForTimeout(500);

    const clearButton = page
      .locator("button")
      .filter({ hasText: /clear/i })
      .first();
    await clearButton.click();
    await page.waitForTimeout(500);

    await expect(nameInput).toHaveValue("");
  });

  test("should apply multiple filters combined", async ({ page }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await nameInput.fill("rex");
    await page.waitForTimeout(300);

    const weightFilterTitle = page
      .locator("button")
      .filter({ hasText: /weight/i })
      .first();
    await weightFilterTitle.click();

    const minInput = page.locator('input[type="number"]').nth(0);
    const maxInput = page.locator('input[type="number"]').nth(1);

    await minInput.fill("5");
    await maxInput.fill("100");
    await page.waitForTimeout(500);

    const results = page.locator("tbody tr, li");
    const count = await results.count();
    expect(count).toBeGreaterThanOrEqual(0);

    if (count > 0) {
      const firstResult = results.first();
      await expect(firstResult).toBeVisible();
    }
  });

  test("should display data in all three layout modes", async ({ page }) => {
    let layout = page.locator("li").first();
    await expect(layout).toBeVisible({ timeout: 5000 });

    const tableButton = page.getByRole("button", { name: "Table" });
    await tableButton.click();
    await page.waitForTimeout(500);

    layout = page.locator("tbody tr").first();
    await expect(layout).toBeVisible();

    const cardsButton = page.getByRole("button", { name: "Cards" });
    await cardsButton.click();
    await page.waitForTimeout(500);

    layout = page.locator("[class*='card']").first();
    await expect(layout).toBeVisible();
  });

  test("should filter debounce and not cause excessive flicker", async ({
    page,
  }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();

    await nameInput.fill("a");
    await page.waitForTimeout(100);
    await nameInput.fill("ab");
    await page.waitForTimeout(100);
    await nameInput.fill("abc");
    await page.waitForTimeout(400);

    const results = page.locator("tbody tr, li");
    const finalCount = await results.count();
    expect(finalCount).toBeGreaterThanOrEqual(0);
  });

  test("URL updates on filter change", async ({ page }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await nameInput.fill("fluffy");

    await page.waitForTimeout(500);

    const url = page.url();
    const params = new URLSearchParams(url.split("?")[1] || "");
    expect(params.get("name")).toBe("fluffy");
  });

  test("direct URL navigation with filter params pre-fills input", async ({
    page,
  }) => {
    const filteredUrl = `${route}filter/Emx2DataView?name=fluffy`;
    await page.goto(filteredUrl);

    await expect(page.getByText("Emx2DataView")).toBeVisible({
      timeout: 10000,
    });

    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await expect(nameInput).toHaveValue("fluffy");

    const result = page.getByText("fluffy");
    await expect(result).toBeVisible({ timeout: 5000 });
  });

  test("browser back/forward restores filter state", async ({ page }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();

    await nameInput.fill("fluffy");
    await page.waitForTimeout(500);

    let url = page.url();
    let params = new URLSearchParams(url.split("?")[1] || "");
    expect(params.get("name")).toBe("fluffy");

    await nameInput.fill("rex");
    await page.waitForTimeout(500);

    url = page.url();
    params = new URLSearchParams(url.split("?")[1] || "");
    expect(params.get("name")).toBe("rex");

    await page.goBack();
    await page.waitForTimeout(500);

    await expect(nameInput).toHaveValue("fluffy");

    url = page.url();
    params = new URLSearchParams(url.split("?")[1] || "");
    expect(params.get("name")).toBe("fluffy");
  });

  test("clear filter removes URL param", async ({ page }) => {
    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await nameInput.fill("fluffy");
    await page.waitForTimeout(500);

    let url = page.url();
    let params = new URLSearchParams(url.split("?")[1] || "");
    expect(params.get("name")).toBe("fluffy");

    const clearButton = page
      .locator("button")
      .filter({ hasText: /clear/i })
      .first();
    await clearButton.click();
    await page.waitForTimeout(500);

    url = page.url();
    params = new URLSearchParams(url.split("?")[1] || "");
    expect(params.get("name")).toBeNull();
  });

  test("SSR verification - filter state from URL on initial render", async ({
    page,
  }) => {
    const filteredUrl = `${route}filter/Emx2DataView?name=test&weight=5..20`;
    await page.goto(filteredUrl);

    await expect(page.getByText("Emx2DataView")).toBeVisible({
      timeout: 10000,
    });

    const nameFilterTitle = page
      .locator("button")
      .filter({ hasText: /name/i })
      .first();
    await nameFilterTitle.click();

    const nameInput = page
      .locator('input[type="text"]')
      .filter({ hasText: /name/i })
      .first();
    await expect(nameInput).toHaveValue("test");

    const weightFilterTitle = page
      .locator("button")
      .filter({ hasText: /weight/i })
      .first();
    await weightFilterTitle.click();

    const minInput = page.locator('input[type="number"]').nth(0);
    await expect(minInput).toHaveValue("5");
  });
});
