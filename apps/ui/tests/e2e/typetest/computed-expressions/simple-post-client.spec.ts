import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
import {
  createSchemaFromTemplate,
  deleteSchema,
  gql,
  RUN_ID,
  signinAdmin,
} from "../../e2eUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

// the 'country name' computed expression calls simplePostClient with a hardcoded
// schemaId ('type test'), because the backend re-evaluates it on save and needs
// one, so this test must run against that schema rather than a random one
const SCHEMA = "type test";
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
const GRAPHQL = `${route}${SCHEMA_PATH}/graphql`;
const FIRST_NAME = `Grace${RUN_ID}`;
const LAST_NAME = `Hopper${RUN_ID}`;

let api: APIRequestContext;
let createdSchema = false;
let skipReason: string | undefined;

test.beforeAll(async () => {
  api = await apiRequest.newContext({
    storageState: { cookies: [], origins: [] },
  });
  await signinAdmin(api, route);

  const { _schemas } = await gql(api, `${route}graphql`, `{ _schemas { id } }`);
  if (!_schemas.some((schema: { id: string }) => schema.id === SCHEMA)) {
    await createSchemaFromTemplate(api, route, SCHEMA, "TYPE_TEST");
    createdSchema = true;
  }

  // a 'type test' schema created before the lookup columns were added cannot run this test
  try {
    await gql(
      api,
      GRAPHQL,
      `{ ComputedExpressions(limit: 1) { countryName } }`
    );
  } catch {
    skipReason = `'${SCHEMA}' schema predates the simplePostClient columns, recreate it`;
  }
});

test.afterAll(async () => {
  if (createdSchema) {
    await deleteSchema(api, route, SCHEMA);
  } else if (!skipReason) {
    // shared schema: only remove the row this test added
    const data = await gql(
      api,
      GRAPHQL,
      `query rows($filter: ComputedExpressionsFilter) {
        ComputedExpressions(filter: $filter) { id }
      }`,
      { filter: { firstName: { equals: FIRST_NAME } } }
    ).catch(() => undefined);
    const rows = data?.ComputedExpressions ?? [];
    if (rows.length) {
      await gql(
        api,
        GRAPHQL,
        `mutation remove($rows: [ComputedExpressionsInput]) {
          delete(ComputedExpressions: $rows) { message }
        }`,
        { rows }
      ).catch((error) => console.error("could not delete test row:", error));
    }
  }
  await api.dispose();
});

test.beforeEach(async ({ page }) => {
  test.skip(!!skipReason, skipReason);
  await page.goto(route);
  await page.getByRole("button", { name: "Signin" }).click();
  await page.getByRole("textbox", { name: "Username" }).fill("admin");
  await page.getByRole("textbox", { name: "Password" }).fill("admin");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
  await page.goto(`${route}${SCHEMA_PATH}/ComputedExpressions`);
});

test("a computed expression can look up a value using simplePostClient", async ({
  page,
}) => {
  await page.getByRole("button", { name: "Add ComputedExpressions" }).click();

  const countryCode = page.getByRole("textbox", { name: "country code" });
  const countryName = page.getByRole("textbox", { name: "country name" });
  await expect(countryName).toBeDisabled();
  await expect(countryName).toHaveValue("");

  // the Options table of the type test demo data maps AT -> Austria, BE -> Belgium
  await countryCode.fill("AT");
  await expect(countryName).toHaveValue("Austria");

  await countryCode.fill("BE");
  await expect(countryName).toHaveValue("Belgium");

  await page
    .getByRole("textbox", { name: "first name Required" })
    .fill(FIRST_NAME);
  await page
    .getByRole("textbox", { name: "last name Required" })
    .fill(LAST_NAME);

  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByRole("button", { name: "Close modal", exact: true }).click();

  // the saved value is recomputed by the backend's own simplePostClient
  const row = page.getByRole("row").filter({ hasText: FIRST_NAME });
  await expect(
    row.getByRole("cell", { name: "Belgium", exact: true })
  ).toBeVisible();
});
