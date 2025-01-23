import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test("it should render the form", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.goto("http://localhost:3000/");
  await page.getByRole("link", { name: "Form" }).click();
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("test");
  await expect(page.getByText("test")).toBeVisible();
  await expect(page.getByRole("main")).toContainText("test");
  await page.goto("http://localhost:3000/Form.story");
  await expect(page.getByRole("main")).toContainText("name");
  await expect(page.getByRole("main")).toContainText("the name");
  await expect(page.getByRole("main")).toContainText("date");
  await expect(page.getByRole("main")).toContainText("name");
  await expect(page.getByRole("main")).toContainText("Required");
  await expect(page.getByRole("main")).toContainText("the name");
  await expect(page.getByLabel("name")).toBeVisible();
});

test("it should handle input", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.getByLabel("name").click({ delay: 500 }); // wait for hydration to complete
  await page.getByLabel("name").pressSequentially("test");
  await expect(page.getByLabel("name")).toHaveValue("test");
});
