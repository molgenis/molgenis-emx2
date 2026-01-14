import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}input/Date.story`);
  await page.getByRole("heading", { name: "Date" }).click({ delay: 1000 });
});

test("clicking the clear btn should empty the input and show the placeholder", async ({
  page,
}) => {
  await expect(page.locator('[data-test-id="dp-input"]')).toHaveValue(
    /\d{4}-\d{2}-\d{2}/
  );
  await page.getByRole("button", { name: "Clear value" }).click();
  await expect(page.locator('[data-test-id="dp-input"]')).toBeEmpty();
  await expect(page.locator('[data-test-id="dp-input"]')).toHaveAttribute(
    "placeholder",
    "yyyy-MM-dd"
  );
});
