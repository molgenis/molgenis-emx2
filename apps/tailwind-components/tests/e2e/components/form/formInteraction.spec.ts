import { test, expect } from "@playwright/test";
import playwrightConfig from "~/playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}Form.story?schema=pet+store&table=Pet`);
  await page.getByText("Jump to", { exact: true }).click({ delay: 300 });
});

test("it should update the model value when a field is filled out", async ({
  page,
}) => {
  await page.getByLabel("name Required", { exact: true }).click();
  await page.getByLabel("name Required", { exact: true }).fill("test");
  await expect(page.getByLabel("name Required", { exact: true })).toHaveValue(
    "test"
  );
});

test("it should not jump around when selecting a checkbox", async ({
  page,
}) => {
  await page.getByRole("link", { name: "details" }).click();
  await page.getByText("red", { exact: true }).click();
  await page
    .locator("#tags-form-field-input-checkbox-group")
    .getByText("colors")
    .click();
  await expect(page.locator("#tags-form-field")).toContainText("red");
});
