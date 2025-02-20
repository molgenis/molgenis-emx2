import { expect, test } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";
const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}input/Hyperlink.story`);
  await page
    .getByRole("textbox", { name: "Input a hyperlink" })
    .click({ delay: 500 });
});

test("the inputHyperLink", async ({ page }) => {
  await expect(page.getByText("Invalid hyperlink")).not.toBeVisible();
  await page.getByRole("textbox", { name: "Input a hyperlink" }).fill("blaat");
  await page.getByRole("textbox", { name: "Input a hyperlink" }).blur();
  await expect(page.getByText("Invalid hyperlink")).toBeVisible();
  await page.getByPlaceholder("Input a hyperlink").clear();
  await page.getByPlaceholder("Input a hyperlink").fill("https://molgenis.net");
  await page.getByRole("textbox", { name: "Input a hyperlink" }).blur();
  await expect(page.getByText("Invalid hyperlink")).not.toBeVisible();
});
