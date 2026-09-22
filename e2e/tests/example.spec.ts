import { test, expect } from "@playwright/test";


test("has title", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle("emx2-central");
});

test("get started link", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "Components (for developers)" }).click();
  await expect(page).toHaveURL(/.*molgenis-components/);
});