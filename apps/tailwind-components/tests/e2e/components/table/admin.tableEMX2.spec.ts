import { expect, test } from "@nuxt/test-utils/playwright";
import { request as apiRequest } from "@playwright/test";
import type { APIRequestContext } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
import {
  createSchemaFromTemplate,
  deleteSchema,
  RUN_ID,
  signinAdmin,
} from "../../../../../ui/tests/e2e/e2eUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/tailwind-components/#/";

test.describe.configure({ mode: "serial" });

test.use({ storageState: "playwright/.auth/user.json" });

let api: APIRequestContext;
const SCHEMA = `tableEMX2 test ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
// the toolbar switches layout on its own width (the @5xl/toolbar container query)
const COMPACT_VIEWPORT = { width: 1280, height: 900 };
const WIDE_VIEWPORT = { width: 1920, height: 1080 };

test.beforeAll(async () => {
  api = await apiRequest.newContext({
    storageState: { cookies: [], origins: [] },
  });
  await signinAdmin(api, route);
  await createSchemaFromTemplate(api, route, SCHEMA, "PET_STORE");
});

test.afterAll(async () => {
  await deleteSchema(api, route, SCHEMA);
  await api.dispose();
});

test("the row should be removed from the table after deletion (compact toolbar)", async ({
  page,
  goto,
}) => {
  await page.setViewportSize(COMPACT_VIEWPORT);
  await goto(`${route}table/EMX2.story?schema=${SCHEMA_PATH}&table=Category`, {
    waitUntil: "hydration",
  });
  await expect(page.getByText("TableEMX2").first()).toBeVisible();
  await expect(page.getByLabel("Schema:")).toHaveValue(SCHEMA);
  await page.getByRole("checkbox", { name: "Can insert:" }).check();
  await expect(
    page.getByRole("button", { name: "Add Category" })
  ).toBeVisible();

  // create row to delete
  await page.getByRole("button", { name: "Add Category" }).click();
  await page.getByRole("textbox", { name: "name Required" }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill("deltest");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByText(`inserted Category`)).toBeVisible();
  await page.getByRole("button", { name: "Cancel" }).click();

  // delete row
  await page.getByRole("checkbox", { name: "Can delete:" }).check();
  await expect(
    page.getByRole("searchbox", { name: "Search Category" })
  ).toBeHidden();
  await page.getByRole("button", { name: "Search", exact: true }).click();
  await page
    .getByRole("searchbox", { name: "Search Category" })
    .fill("deltest");
  await expect(
    page.getByRole("cell", { name: "deltest", exact: true })
  ).toBeVisible();
  await page
    .locator(".w-5.h-5.hover\\:cursor-pointer.accent-theme.block")
    .first()
    .click();
  await page.getByRole("button", { name: "Close search" }).click();
  await page.getByRole("button", { name: "More actions" }).click();
  await page.getByRole("button", { name: "Delete selection" }).click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "deltest", exact: true })
  ).toBeHidden();
});

test("the row should be copied and added to the table after copying (wide toolbar)", async ({
  page,
  goto,
}) => {
  await page.setViewportSize(WIDE_VIEWPORT);
  await goto(`${route}table/EMX2.story?schema=${SCHEMA_PATH}&table=Pet`, {
    waitUntil: "hydration",
  });
  await expect(page.getByText("TableEMX2").first()).toBeVisible();
  await expect(page.getByLabel("Schema:")).toHaveValue(SCHEMA);
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
  await expect(page.getByRole("button", { name: "More actions" })).toBeHidden();
  await page.getByRole("searchbox", { name: "Search Pet" }).fill("copy cat");
  await expect(
    page
      .locator("div")
      .filter({ hasText: /^copy cat$/ })
      .first()
  ).toBeVisible();
});
