import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {});

test.afterEach(async ({ page }) => {
  const cellLocator = page.getByRole("cell", { name: /^.*foobar.*/i });
  if ((await cellLocator.count()) > 0 && (await cellLocator.isVisible())) {
    await cellLocator.hover();

    const deleteButton = page.getByRole("button", {
      name: /^.*deletefoobar.*/i,
    });
    if ((await deleteButton.count()) > 0 && (await deleteButton.isVisible())) {
      await deleteButton.click();
    }

    const confirmButton = page.getByRole("button", { name: "Delete" });
    if (
      (await confirmButton.count()) > 0 &&
      (await confirmButton.isVisible())
    ) {
      await confirmButton.click();
    }
  }
});

test.describe("period input type", () => {
  test("it should be possible to create a period", async ({ page }) => {
    await page.goto(`${route}type%20test/Types`);
    await page.getByRole("button", { name: "Add Types" }).click();
    await page.getByRole("textbox", { name: "period type" }).click();
    await page.getByRole("textbox", { name: "period type" }).fill("test");
    await page.getByRole("textbox", { name: "period type" }).press("Tab");

    await expect(page.getByText("errorInvalid Period: must")).toBeVisible();

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
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_0"]')
      .click();
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_0"]')
      .click();
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_0"]')
      .fill("string1");
    await page
      .locator('[id="type test-Types-stringArrayType-form-field"]')
      .getByRole("button", { name: "Add an additional item" })
      .click();
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_1"]')
      .click();
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_1"]')
      .fill("string2");
    await page
      .locator('[id="type test-Types-stringArrayType-form-field"]')
      .getByRole("button", { name: "Add an additional item" })
      .click();
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_2"]')
      .click();
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_2"]')
      .fill("string3");
    await page.getByRole("button", { name: "Remove item" }).nth(1).click();
    await expect(
      page.locator('[id="type test-Types-stringArrayType-form-field-input_0"]')
    ).toBeVisible();
    await page
      .locator('[id="type test-Types-stringArrayType-form-field-input_1"]')
      .click();
    await expect(
      page.locator('[id="type test-Types-stringArrayType-form-field-input_0"]')
    ).toHaveValue("string1");
    await expect(
      page.locator('[id="type test-Types-stringArrayType-form-field-input_1"]')
    ).toHaveValue("string3");
  });
});

test.describe("Insert type record with only required fields", () => {
  test("it should be possible to create a record with only required fields", async ({
    page,
  }) => {
    await page.goto(`${route}type%20test/Types`);
    await page.getByRole("button", { name: "Add Types" }).click();
    await page.getByRole("textbox", { name: "string type Required" }).click();
    await page
      .getByRole("textbox", { name: "string type Required" })
      .fill("foobar");
    await page.getByRole("button", { name: "Save", exact: true }).click();
    // wait for save to complete
    await page.waitForTimeout(3000);
    await page.getByRole("button", { name: "Cancel" }).click();
    await expect(
      page.getByRole("cell", { name: /^.*foobar.*/i })
    ).toBeVisible();
  });
});
