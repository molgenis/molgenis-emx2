import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.getByLabel("name").click({ delay: 500 }); // wait for hydration to complete
});

test("it should render the form", async ({ page }) => {
  await expect(page.getByRole("main")).toContainText("name");
  await expect(page.getByRole("main")).toContainText("the name");
  await expect(page.getByRole("main")).toContainText("date");
  await expect(page.getByLabel("name")).toBeVisible();
});

test("it should handle input", async ({ page }) => {
  await page.getByLabel("name").pressSequentially("test");
  await expect(page.getByLabel("name")).toHaveValue("test");
});

test("it should show the chapters in the legend", async ({ page }) => {
  await expect(
    page.locator("span").filter({ hasText: "details" })
  ).toBeVisible();
  await expect(page.getByText("Heading2", { exact: true })).toBeVisible();
});

test("the legend should show number of errors per chapter (if any)", async ({
  page,
}) => {
  await page.getByLabel("Demo data").selectOption("complex", { force: true });
  // touch the form
  await page.getByLabel("name", { exact: true }).click();
  // skip a required field
  await page.getByLabel("name", { exact: true }).press("Tab");
  await expect(page.locator("span").filter({ hasText: /^2$/ })).toBeVisible();
});

test("clicking on the chapter should scroll to the chapter", async ({
  page,
}) => {
  await page.getByText("Heading2", { exact: true }).click();
  await expect(page.getByRole("heading", { name: "heading2" })).toBeVisible();
});
