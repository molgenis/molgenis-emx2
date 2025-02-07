import { expect, test } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";
const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}input/Email.story`);
  await page
    .getByRole("textbox", { name: "Input an email address" })
    .click({ delay: 500 });
});

test("the inputEmail", async ({ page }) => {
  await page
    .getByRole("textbox", { name: "Input an email address" })
    .fill("test");
  await page.getByRole("textbox", { name: "Input an email address" }).blur();
  await expect(page.getByText("Invalid email address")).toBeVisible();
  await page.getByPlaceholder("Input an email address").clear();
  await page
    .getByRole("textbox", { name: "Input an email address" })
    .fill("test@molgenis.net");
  await page.getByRole("textbox", { name: "Input an email address" }).blur();
  await expect(page.getByText("Invalid email address")).not.toBeVisible();
});
