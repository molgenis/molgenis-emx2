import { test, expect } from "@playwright/test";

const localhost = "http://localhost:8080/";

test("has title", async ({ page }) => {
  await page.goto(localhost);
  await expect(page).toHaveTitle("emx2-central");
});

test("get started link", async ({ page }) => {
  await page.goto(localhost);
  await page.getByRole("link", { name: "Components (for developers)" }).click();
  await expect(page).toHaveURL(/.*molgenis-components/);
});

test("login as admin success", async ({ page }) => {
  await page.goto("http://localhost:8080/apps/central/");
  await page.goto("http://localhost:8080/apps/central/#/");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByPlaceholder("Enter username").click();
  await page.getByPlaceholder("Enter username").fill("admin");
  await page.getByPlaceholder("Enter username").press("Tab");
  await page.getByPlaceholder("Password").fill("admin");
  await page.getByPlaceholder("Password").press("Enter");
  await page.getByRole("link", { name: "_SYSTEM_" }).click();
});