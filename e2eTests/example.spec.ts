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

test("test crud", async ({ page }) => {
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
  await page.getByRole("link", { name: "pet store" }).click();
  await page.getByRole("link", { name: "Pet", exact: true }).click();
  await page.getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "name(required)name is required the name" })
    .locator("#Pet-edit-modal-name")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "name(required)name is required the name" })
    .locator("#Pet-edit-modal-name")
    .fill("lama");
  await page.locator("#Pet-edit-modal-category").getByRole("textbox").click();
  await page
    .getByRole("row", {
      name: "Select   someaa3e8aa6-25b4-4c3f-9804-0552043f4bacthing Wurm"
    })
    .getByRole("button", { name: "Select" })
    .click();
  await page.getByRole("button", { name: "Next " }).click();
  await page.getByRole("spinbutton").click();
  await page.getByRole("spinbutton").fill("4");
  await page
    .locator("span")
    .filter({ hasText: "required column(required)required column is required" })
    .locator('[id="Pet-edit-modal-required\\ column"]')
    .click();
  await page
    .locator("span")
    .filter({ hasText: "required column(required)required column is required" })
    .locator('[id="Pet-edit-modal-required\\ column"]')
    .fill("4");
  await page.getByRole("button", { name: "Save Pet" }).click();
  await page
    .getByRole("row", { name: "   lama Wurm 4 4" })
    .getByRole("button", { name: "" })
    .click();
  await page.getByText("Previous Next Close Save draft Save Pet").click();
  await page.getByRole("button", { name: "details" }).click();
  await page.getByRole("spinbutton").dblclick();
  await page.getByRole("spinbutton").click();
  await page.getByRole("spinbutton").fill("48");
  await page.getByRole("button", { name: "Save Pet" }).click();
  await page
    .getByRole("row", { name: "   lama Wurm 48 4" })
    .getByRole("button", { name: "" })
    .click();
  await page.getByRole("button", { name: "Delete" }).click();
});
