import { Page } from "@playwright/test";

export async function login(page: Page) {
  await page.goto("/apps/central/#/");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByPlaceholder("Enter username").click();
  await page.getByPlaceholder("Enter username").fill("admin");
  await page.getByPlaceholder("Enter username").press("Tab");
  await page.getByPlaceholder("Password").fill("admin");
  await page.getByPlaceholder("Password").press("Enter");
}

export async function optionallyRemoveDatabase(
  page: Page,
  databaseName: string
) {
  await page.goto("/apps/central/#/");
  await page.waitForTimeout(1000);
  let deleteButton = await page.getByRole("row", {
    name: "  " + databaseName,
    exact: true,
  });
  if ((await deleteButton.count()) === 1) {
    deleteButton.getByRole("button").nth(1).click();
    await page.getByRole("button", { name: "Delete database" }).click();
    await page.getByRole("button", { name: "Close" }).nth(1).click();
  }
}

export async function createDatabase(
  page: Page,
  databaseName: string,
  template: string,
  useDemoData: boolean
) {
  await page.goto("/apps/central/#/");
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill(databaseName);
  await page.getByLabel("template").selectOption(template);
  if (useDemoData) {
    await page.getByLabel("Yes").check();
  }
  await page.getByRole("button", { name: "Create database" }).click();
  await page.getByText("Close").click();
}
