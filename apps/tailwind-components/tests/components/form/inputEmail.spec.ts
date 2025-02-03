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
  await expect(page.getByText("Error:")).not.toContainText(
    "Error: Invalid email"
  );
  await page.fill("#input-email", "blaat");
  await expect(page.getByText("Error: Invalid email")).toContainText(
    "Error: Invalid email"
  );
  await page.getByPlaceholder("example@molgenis.net").clear();
  await page.fill("#input-email", "test@molgenis.net");
  await expect(page.getByText("Error:")).not.toContainText(
    "Error: Invalid email"
  );
});
