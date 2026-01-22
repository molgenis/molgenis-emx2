import { expect, test } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}input/UUID.story`);
});

test("the inputUUID", async ({ page }) => {
  await page.getByRole("textbox", { name: "Please enter a UUID" }).fill("test");
  await page.getByRole("textbox", { name: "Please enter a UUID" }).blur();
  await expect(
    page.getByText("Invalid UUID: should be a valid UUID format")
  ).toBeVisible();
  await page.getByRole("textbox", { name: "Please enter a UUID" }).clear();
  await page
    .getByRole("textbox", { name: "Please enter a UUID" })
    .fill("123e4567-e89b-12d3-a456-426614174000");
  await page.getByRole("textbox", { name: "Please enter a UUID" }).blur();
  await expect(
    page.getByText("Invalid UUID: should be a valid UUID format")
  ).not.toBeVisible();
});
