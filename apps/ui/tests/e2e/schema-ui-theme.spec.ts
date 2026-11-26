import type { APIRequestContext } from "@playwright/test";
import { request as apiRequest, expect, test } from "@playwright/test";
import playwrightConfig from "../../playwright.config";
import {
  createSchemaFromTemplate,
  deleteSchema,
  gql,
  RUN_ID,
  signinAdmin,
} from "./e2eUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

let api: APIRequestContext;

const SCHEMA = `schema ui theme test ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);

test.describe.configure({ mode: "serial" });

// the theme switch falls back to the browser preference, pin it so the start state is known
test.use({ colorScheme: "light" });

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await signinAdmin(api, route);
  await createSchemaFromTemplate(api, route, SCHEMA, "PET_STORE");
  await gql(
    api,
    `${route}${SCHEMA_PATH}/graphql`,
    `mutation change($settings: [MolgenisSettingsInput]) {
      change(settings: $settings) {
        message
      }
    }`,
    { settings: [{ key: "SCHEMA_UI_THEME", value: "dark" }] }
  );
});

test.afterAll(async () => {
  await deleteSchema(api, route, SCHEMA);
  await api.dispose();
});

test("the schema ui theme is applied when navigating to the schema in the app", async ({
  page,
}) => {
  const html = page.locator("html");

  await page.goto(route, { waitUntil: "load" });
  // the test needs a light start, i.e. no (dark) UI_THEME set on the server
  await expect(html).toHaveAttribute("data-theme", "light");

  // client side navigation, the header (and theme switch) stays mounted
  await page.getByText(SCHEMA, { exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`${SCHEMA_PATH}/?$`));
  await expect(html).toHaveAttribute("data-theme", "dark");
  await expect(
    page.getByRole("button", { name: "Toggle theme" })
  ).toHaveAttribute("aria-pressed", "true");

  // and back again, the system theme applies
  await page.goBack();
  await expect(page).toHaveURL(route);
  await expect(html).toHaveAttribute("data-theme", "light");
});

test("a theme chosen with the theme switch wins over the schema ui theme", async ({
  page,
}) => {
  await page.goto(route, { waitUntil: "load" });
  const html = page.locator("html");
  await expect(html).toHaveAttribute("data-theme", "light");

  // choosing light explicitly stores it in the cookie
  await page.getByRole("button", { name: "Toggle theme" }).click();
  await expect(html).toHaveAttribute("data-theme", "dark");
  await page.getByRole("button", { name: "Toggle theme" }).click();
  await expect(html).toHaveAttribute("data-theme", "light");

  await page.getByText(SCHEMA, { exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`${SCHEMA_PATH}/?$`));
  await expect(html).toHaveAttribute("data-theme", "light");
});
