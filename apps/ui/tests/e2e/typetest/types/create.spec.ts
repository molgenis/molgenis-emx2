import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
import {
  createSchemaFromTemplate,
  deleteSchema,
  RUN_ID,
  signinAdmin,
} from "../../testSchema";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

/* its own copy of the type test model, so the row saved below cannot move the
   totals filter-count-parity.spec.ts asserts against the seeded schema */
const SCHEMA = `types create ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
const arrayField = `${SCHEMA}-Types-stringArrayType-form-field`;

let api: APIRequestContext;

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await signinAdmin(api, route);
  await createSchemaFromTemplate(api, route, SCHEMA, "TYPE_TEST");
});

test.afterAll(async () => {
  await deleteSchema(api, route, SCHEMA);
  await api.dispose();
});

test.describe("period input type", () => {
  test("it should be possible to create a period", async ({ page }) => {
    await page.goto(`${route}${SCHEMA_PATH}/Types`);
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
    await expect(page.getByText("errorInvalid Period: must")).toBeHidden();
  });
});

test.describe("array input types", () => {
  test("it should be possible to create and remove multiple items", async ({
    page,
  }) => {
    await page.goto(`${route}${SCHEMA_PATH}/Types`);
    await page.getByRole("button", { name: "Add Types" }).click();
    await page.locator(`[id="${arrayField}-input_0"]`).click();
    await page.locator(`[id="${arrayField}-input_0"]`).click();
    await page.locator(`[id="${arrayField}-input_0"]`).fill("string1");
    await page
      .locator(`[id="${arrayField}"]`)
      .getByRole("button", { name: "Add an additional item" })
      .click();
    await page.locator(`[id="${arrayField}-input_1"]`).click();
    await page.locator(`[id="${arrayField}-input_1"]`).fill("string2");
    await page
      .locator(`[id="${arrayField}"]`)
      .getByRole("button", { name: "Add an additional item" })
      .click();
    await page.locator(`[id="${arrayField}-input_2"]`).click();
    await page.locator(`[id="${arrayField}-input_2"]`).fill("string3");
    await page.getByRole("button", { name: "Remove item" }).nth(1).click();
    await expect(page.locator(`[id="${arrayField}-input_0"]`)).toBeVisible();
    await page.locator(`[id="${arrayField}-input_1"]`).click();
    await expect(page.locator(`[id="${arrayField}-input_0"]`)).toHaveValue(
      "string1"
    );
    await expect(page.locator(`[id="${arrayField}-input_1"]`)).toHaveValue(
      "string3"
    );
  });
});

test.describe("Insert type record with only required fields", () => {
  test("it should be possible to create a record with only required fields", async ({
    page,
  }) => {
    await page.goto(`${route}${SCHEMA_PATH}/Types`);
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

    await page.getByRole("searchbox", { name: "Search Types" }).click();
    await page.getByRole("searchbox", { name: "Search Types" }).fill("foobar");
    await page.getByRole("row").nth(1).hover();
    await page
      .getByRole("button", { name: 'delete {"stringType":"foobar' })
      .click();
    await page.getByRole("button", { name: "Delete", exact: true }).click();
  });
});
