import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
import {
  createSchemaFromTemplate,
  deleteSchema,
  gql,
  signinAdmin,
} from "../../e2eUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

// the 'tw-menu' setting comes from data/_settings/typetest, which the TypeTest
// profile loads, so this test runs against the 'type test' schema
const SCHEMA = "type test";
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
const GRAPHQL = `${route}${SCHEMA_PATH}/graphql`;

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

  // a 'type test' schema created before the menu setting was added cannot run this test
  const { _settings } = await gql(
    api,
    GRAPHQL,
    `{ _settings(keys: ["tw-menu"]) { key value } }`
  );
  if (
    !_settings.some((setting: { key: string }) => setting.key === "tw-menu")
  ) {
    skipReason = `'${SCHEMA}' schema predates the tw-menu setting, recreate it`;
  }
});

test.afterAll(async () => {
  if (createdSchema) {
    await deleteSchema(api, route, SCHEMA);
  }
  await api.dispose();
});

test.beforeEach(async ({ page }) => {
  test.skip(!!skipReason, skipReason);
  await page.goto(`${route}${SCHEMA_PATH}`);
  // the submenu button only opens once the page is hydrated
  await page.waitForLoadState("networkidle");
});

test("the tw-menu setting replaces the default schema menu", async ({
  page,
}) => {
  await expect(
    page.getByRole("link", { name: "MOLGENIS", exact: true })
  ).toBeVisible();
  // 'Jobs & Scripts' is only in the default schema menu
  await expect(page.getByRole("link", { name: "Jobs & Scripts" })).toHaveCount(
    0
  );
});

test("the menu shows a spaLink, an internalPageLink and an externalLink", async ({
  page,
}) => {
  await expect(
    page.getByRole("link", { name: "Tables", exact: true })
  ).toHaveAttribute("href", /\/type%20test$/);
  await expect(
    page.getByRole("link", { name: "Schema", exact: true })
  ).toHaveAttribute("href", "/type%20test/schema");
  await expect(
    page.getByRole("link", { name: "MOLGENIS", exact: true })
  ).toHaveAttribute("href", "https://www.molgenis.org");
});

test("a submenu shows all three link types", async ({ page }) => {
  await page.getByRole("button", { name: "Link types" }).click();

  await expect(page.getByRole("link", { name: "Types table" })).toHaveAttribute(
    "href",
    /\/type%20test\/Types$/
  );
  await expect(page.getByRole("link", { name: "Up/Download" })).toHaveAttribute(
    "href",
    "/type%20test/updownload"
  );
  await expect(
    page.getByRole("link", { name: "MOLGENIS docs" })
  ).toHaveAttribute("href", "https://molgenis.github.io/molgenis-emx2/");
});

test("a spaLink navigates inside the ui app", async ({ page }) => {
  // a cold dev server may reload the page while it compiles the table page,
  // which drops the click, so open the submenu and click again until it lands
  await expect(async () => {
    await page.getByRole("button", { name: "Link types" }).click();
    await page
      .getByRole("link", { name: "Types table" })
      .click({ timeout: 2000 });
    await expect(page).toHaveURL(/\/type%20test\/Types$/, { timeout: 5000 });
  }).toPass({ timeout: 30000 });
});
