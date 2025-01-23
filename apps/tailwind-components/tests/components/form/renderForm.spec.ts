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
  await expect(page.getByRole("main")).toContainText("Required");
  await expect(page.getByLabel("name")).toBeVisible();
});

test("it should handle input", async ({ page }) => {
  await page.getByLabel("name").pressSequentially("test");
  await expect(page.getByLabel("name")).toHaveValue("test");
  await page.getByRole("heading", { name: "Values" }).click();
  await expect(page.getByRole("definition")).toContainText("test");
});
