import { expect, test } from "@nuxt/test-utils/playwright";
import { request as apiRequest } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
import {
  createSchemaFromTemplate,
  deleteSchema,
  RUN_ID,
  signinAdmin,
} from "../../../../../ui/tests/e2e/testSchema";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/tailwind-components/#/";

test.describe.configure({ mode: "serial" });

test.use({ storageState: "playwright/.auth/user.json" });

let api: APIRequestContext;
const SCHEMA = `tableEMX2 test ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await signinAdmin(api, route);
  await createSchemaFromTemplate(api, route, SCHEMA, "PET_STORE");
});

test.afterAll(async () => {
  await deleteSchema(api, route, SCHEMA);
  await api.dispose();
});

test("the row should be removed from the table after deletion", async ({
  page,
  goto,
}) => {
  await goto(`${route}table/EMX2.story?schema=pet+store&table=Category`, {
    waitUntil: "hydration",
  });
  await expect(page.getByText("TableEMX2").first()).toBeVisible();
  await expect(page.getByLabel("Schema:")).toHaveValue("pet store");
  await page.getByRole("checkbox", { name: "Can insert:" }).check();
  await expect(
    page.getByRole("button", { name: "Add Category" })
  ).toBeVisible();

  // create row to delete
  await page.getByRole("button", { name: "Add Category" }).click();
  await page.getByRole("textbox", { name: "name Required" }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill("deltest");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByRole("button", { name: "Cancel" }).click();

  // delete row
  await page.getByRole("checkbox", { name: "Can delete:" }).check();
  await page.getByRole("searchbox", { name: "Search Category" }).click();
  await page
    .getByRole("searchbox", { name: "Search Category" })
    .fill("deltest");

  await page.getByRole("cell", { name: "deltest" }).hover();
  await page.getByRole("button", { name: 'delete {"name":"deltest"}' }).click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "view row details deltest" })
  ).toBeHidden();
});

test("the row should be copied and added to the table after copying", async ({
  page,
  goto,
}) => {
  await goto(`${route}table/EMX2.story?schema=pet+store&table=Pet`, {
    waitUntil: "hydration",
  });
  await expect(page.getByText("TableEMX2").first()).toBeVisible();
  await expect(page.getByLabel("Schema:")).toHaveValue("pet store");
  await page.getByRole("checkbox", { name: "Can insert:" }).check();
  await expect(page.getByRole("button", { name: "Add Pet" })).toBeVisible();

  // copy Pooky
  await page.getByRole("cell", { name: "pooky" }).hover();
  await expect(
    page.getByRole("button", { name: 'copy {"name":"pooky"}' })
  ).toBeVisible();
  await page.getByRole("button", { name: 'copy {"name":"pooky"}' }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill("copy cat");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByRole("button", { name: "Cancel" }).click();
  await page.getByRole("searchbox", { name: "Search Pet" }).fill("copy cat");
  await expect(
    page
      .locator("div")
      .filter({ hasText: /^copy cat$/ })
      .first()
  ).toBeVisible();
});
