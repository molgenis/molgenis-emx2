import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
import {
  createSchemaFromTemplate,
  deleteSchema,
  RUN_ID,
  signinAdmin,
} from "../../e2eUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

const SCHEMA = `computed expressions ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
const FIRST_NAME = `Ada${RUN_ID}`;
const LAST_NAME = `Lovelace${RUN_ID}`;

let api: APIRequestContext;

test.beforeAll(async () => {
  api = await apiRequest.newContext({
    storageState: { cookies: [], origins: [] },
  });
  await signinAdmin(api, route);
  await createSchemaFromTemplate(api, route, SCHEMA, "TYPE_TEST");
});

test.afterAll(async () => {
  await deleteSchema(api, route, SCHEMA);
  await api.dispose();
});

test.beforeEach(async ({ page }) => {
  // the new schema is not visible to anonymous users, so sign in first
  await page.goto(route);
  await page.getByRole("button", { name: "Signin" }).click();
  await page.getByRole("textbox", { name: "Username" }).fill("admin");
  await page.getByRole("textbox", { name: "Password" }).fill("admin");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
  await page.goto(`${route}${SCHEMA_PATH}/ComputedExpressions`);
});

test("the full name is computed from the first and last name and saved", async ({
  page,
}) => {
  await page.getByRole("button", { name: "Add ComputedExpressions" }).click();

  const fullName = page.getByRole("textbox", { name: "full name Required" });
  await expect(fullName).toBeDisabled();

  await page
    .getByRole("textbox", { name: "first name Required" })
    .fill(FIRST_NAME);
  await page
    .getByRole("textbox", { name: "last name Required" })
    .fill(LAST_NAME);

  await expect(fullName).toHaveValue(`${FIRST_NAME} ${LAST_NAME}`);

  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByRole("button", { name: "Close modal", exact: true }).click();

  const row = page.getByRole("row").filter({ hasText: FIRST_NAME });
  await expect(
    row.getByRole("cell", { name: FIRST_NAME, exact: true })
  ).toBeVisible();
  await expect(
    row.getByRole("cell", { name: LAST_NAME, exact: true })
  ).toBeVisible();
  await expect(
    row.getByRole("cell", { name: `${FIRST_NAME} ${LAST_NAME}`, exact: true })
  ).toBeVisible();
});
