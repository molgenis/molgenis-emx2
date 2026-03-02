import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {});

test.describe("entity details page", () => {
  test("it should be possible to view row details", async ({ page }) => {
    await page.goto(`${route}pet%20store/Pet`);
    const cellLocator = await page.getByRole("cell", { name: /^.*spike.*/i });
    await cellLocator.hover();
    await page.getByRole("button", { name: "view row details" }).click();
    await expect(
      page.locator("div").filter({ hasText: "jerry" }).nth(4)
    ).toBeVisible();
    await expect(page.getByText("name")).toBeVisible();
    await expect(
      page.locator("span").filter({ hasText: "jerry" })
    ).toBeVisible();
    await expect(page.getByText("status")).toBeVisible();
    await expect(page.getByText("sold")).toBeVisible();
    await expect(page.getByText("weight")).toBeVisible();
    await expect(
      page.getByRole("definition").filter({ hasText: "15.7" })
    ).toBeVisible();
  });
});
