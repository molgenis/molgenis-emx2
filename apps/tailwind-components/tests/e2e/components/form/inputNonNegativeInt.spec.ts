import { expect, test } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}input/NonNegativeInt.story`);
});

const errMessage = "Invalid value: must be value from 0 to 2147483647";
const textBoxName = "Input a non negative int";

test("the non negative int", async ({ page }) => {
  await expect(page.getByText(errMessage)).not.toBeVisible();
  await page.getByRole("textbox", { name: textBoxName }).fill("-1");
  await page.getByRole("textbox", { name: textBoxName }).blur();
  await expect(page.getByText(errMessage)).toBeVisible();
  await page.getByRole("textbox", { name: textBoxName }).clear();
  await page.getByRole("textbox", { name: textBoxName }).fill("0");
  await page.getByRole("textbox", { name: textBoxName }).blur();
  await expect(page.getByText(errMessage)).not.toBeVisible();
  await page.getByRole("textbox", { name: textBoxName }).clear();
  await page.getByRole("textbox", { name: textBoxName }).fill("2147483648");
  await page.getByRole("textbox", { name: textBoxName }).blur();
  await expect(page.getByText(errMessage)).toBeVisible();
});
