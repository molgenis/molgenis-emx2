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

// the 'tw-menu' and 'tw-mobile-menu' settings come from data/_settings/typetest,
// which the TypeTest profile loads, so this test runs against the 'type test' schema
const SCHEMA = "type test";
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
const GRAPHQL = `${route}${SCHEMA_PATH}/graphql`;

const SMALL = { width: 390, height: 844 };
const MEDIUM = { width: 1024, height: 768 };

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

  // a 'type test' schema created before the mobile menu setting was added cannot run this test
  const { _settings } = await gql(
    api,
    GRAPHQL,
    `{ _settings(keys: ["tw-mobile-menu"]) { key value } }`
  );
  if (
    !_settings.some(
      (setting: { key: string }) => setting.key === "tw-mobile-menu"
    )
  ) {
    skipReason = `'${SCHEMA}' schema predates the tw-mobile-menu setting, recreate it`;
  }
});

test.afterAll(async () => {
  if (createdSchema) {
    await deleteSchema(api, route, SCHEMA);
  }
  await api.dispose();
});

test.beforeEach(() => {
  test.skip(!!skipReason, skipReason);
});

test.describe("small screen", () => {
  test.use({ viewport: SMALL });

  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}${SCHEMA_PATH}`);
    // the hamburger menu only opens once the page is hydrated
    await page.waitForLoadState("networkidle");
  });

  test("shows the hamburger, an icon-only account button and the tw-mobile-menu row", async ({
    page,
  }) => {
    const header = page.getByTestId("header-small");

    await expect(
      header.getByRole("button", { name: "Open menu" })
    ).toBeVisible();
    await expect(header.getByRole("button", { name: "Signin" })).toBeVisible();
    await expect(header.getByText("Signin", { exact: true })).toHaveClass(
      /sr-only/
    );
    await expect(
      header.getByRole("link", { name: "Types", exact: true })
    ).toBeVisible();
    // 'Link types' is in tw-menu only, so it is not in the mobile row
    await expect(
      header.getByRole("button", { name: "Link types" })
    ).toHaveCount(0);
  });

  test("the hamburger menu shows the full menu with submenus", async ({
    page,
  }) => {
    await page
      .getByTestId("header-small")
      .getByRole("button", { name: "Open menu" })
      .click();

    const menu = page.getByRole("navigation", { name: "Main menu" });
    await expect(menu.getByText("Link types")).toBeVisible();
    await expect(menu.getByRole("link", { name: "Types table" })).toBeVisible();
    await expect(menu.getByRole("link", { name: "Help" })).toBeVisible();
  });

  test("the hamburger menu closes after navigating inside the ui app", async ({
    page,
  }) => {
    await page.goto(`${route}${SCHEMA_PATH}/Types`);
    await page.waitForLoadState("networkidle");

    await page
      .getByTestId("header-small")
      .getByRole("button", { name: "Open menu" })
      .click();
    const menu = page.getByRole("navigation", { name: "Main menu" });
    await menu.getByRole("link", { name: "Tables", exact: true }).click();

    await expect(page).toHaveURL(/\/type%20test$/);
    await expect(menu).toBeHidden();
  });
});

test.describe("medium screen", () => {
  test.use({ viewport: MEDIUM });

  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}${SCHEMA_PATH}`);
    // the 'More' button only opens once the page is hydrated
    await page.waitForLoadState("networkidle");
  });

  test("shows four menu items and puts the rest under 'More'", async ({
    page,
  }) => {
    const header = page.getByTestId("header-medium");

    await expect(header.getByRole("button", { name: "Open menu" })).toHaveCount(
      0
    );
    await expect(
      header.getByRole("link", { name: "Tables", exact: true })
    ).toBeVisible();
    await expect(
      header.getByRole("button", { name: "Link types" })
    ).toBeVisible();
    await expect(header.getByRole("link", { name: "Help" })).toHaveCount(0);

    await header.getByRole("button", { name: "More" }).click();

    await expect(page.getByRole("link", { name: "Help" })).toBeVisible();
  });
});
