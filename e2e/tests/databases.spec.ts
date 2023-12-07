import { test, expect } from "@playwright/test";

test("Create and Remove a new Database", async ({ page }) => {
  await page.goto("http://localhost:8080/apps/central/");
  await page.goto("http://localhost:8080/apps/central/#/");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByPlaceholder("Enter username").click();
  await page.getByPlaceholder("Enter username").fill("admin");
  await page.getByPlaceholder("Enter username").press("Tab");
  await page.getByPlaceholder("Password").fill("admin");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Sign in" })
    .click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("DatabaseTest");
  await page.getByLabel("template").selectOption("PET_STORE");
  await page
    .locator("div")
    .filter({ hasText: /^true$/ })
    .click();
  await page.getByRole("button", { name: "Create database" }).click();
  await page.getByText("Schema DatabaseTest created").click();
  await page.getByText("Close").click();
  await page
    .getByRole("row", { name: "DatabaseTest" })
    .getByRole("button")
    .nth(1)
    .click();
  await page.getByRole("button", { name: "Delete database" }).click();
  await page.getByText("Close").click();
});
