import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {});

test.afterEach(async ({ page }) => {});

test.describe("period input type", () => {
  test("it should be possible to create a period", async ({ page }) => {
    await page.goto(`${route}type%20test/Types`);
    await page.getByRole("button", { name: "Add Types" }).click();
    await page.getByRole("textbox", { name: "period type" }).click();
    await page.getByRole("textbox", { name: "period type" }).fill("test");
    await page.getByRole("textbox", { name: "period type" }).press("Tab");
    await expect(page.getByLabel("error")).toBeVisible();
    await page.getByRole("textbox", { name: "period type" }).dblclick();
    await page.getByRole("textbox", { name: "period type" }).fill("P1Y3M14D");
    await page.getByRole("textbox", { name: "period type" }).press("Tab");
    await expect(
      page.getByRole("textbox", { name: "period type" })
    ).toHaveValue("P1Y3M14D");
    await expect(page.getByLabel("error")).toBeHidden();
  });
});

test.describe("array input types", () => {
  test("it should be possible to create and remove multiple items", async ({
    page,
  }) => {
    await page.goto(`${route}type%20test/Types`);
    await page.getByRole("button", { name: "Add Types" }).click();
    await page.locator("#stringArrayType-form-field-input_0").click();
    await page.locator("#stringArrayType-form-field-input_0").fill("string1");
    await page
      .locator("#stringArrayType-form-field")
      .getByRole("button", { name: "Add an additional item" })
      .click();
    await page.locator("#stringArrayType-form-field-input_1").click();
    await page.locator("#stringArrayType-form-field-input_1").fill("string2");
    await page
      .locator("#stringArrayType-form-field")
      .getByRole("button", { name: "Add an additional item" })
      .click();
    await page.locator("#stringArrayType-form-field-input_2").click();
    await page.locator("#stringArrayType-form-field-input_2").fill("string3");
    await page.getByRole("button", { name: "Remove item" }).nth(1).click();
    await expect(
      page.locator("#stringArrayType-form-field-input_0")
    ).toBeVisible();
    await page.locator("#stringArrayType-form-field-input_1").click();
    await expect(
      page.locator("#stringArrayType-form-field-input_0")
    ).toHaveValue("string1");
    await expect(
      page.locator("#stringArrayType-form-field-input_1")
    ).toHaveValue("string3");
  });
});
