import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test("View ref details", async ({ page }) => {
  await page.goto(`${route}pet%20store/Order`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Order");

  // open the ref
  await page.getByText("pooky").first().click();
  await page.waitForLoadState("networkidle");
  // verify the ref details
  await expect(page.locator("h2")).toContainText("pooky");
  await expect(page.locator("menu")).toContainText("Go to pooky");
  await expect(page.locator("menu")).toContainText("Close");
  // drill down to the ref details
  await page.getByText("cat", { exact: true }).click();
  await page.waitForLoadState("networkidle");
  await expect(page.locator("h2")).toContainText("cat");
  await expect(page.locator("menu")).toContainText("Go to cat");
  // go up a level
  await page.getByRole("button", { name: "Back to pooky" }).click();
  await page.waitForLoadState("networkidle");
  await expect(page.getByRole("heading", { name: "pooky" })).toBeVisible();
  await expect(page.locator("menu")).toContainText("Close");
  // close the ref details
  await page.getByRole("button", { name: "Close", exact: true }).click();
  await page.waitForLoadState("networkidle");
  await expect(page.locator("h1")).toContainText("Order");
});
