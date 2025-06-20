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
});
