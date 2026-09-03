import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext, Page } from "@playwright/test";
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

// shared context so the signin cookie is reused by the follow-up mutations
let api: APIRequestContext;
const USERNAME = "dragonkeeper";
const PASSWORD = "dragonkeeper";
const SCHEMA = `permissions test ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);

test.describe.configure({ mode: "serial" });

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await signinAdmin(api, route);
  await createSchemaFromTemplate(api, route, SCHEMA, "PET_STORE");
  await dropAnonymousFromTestSchema();
  await addPasswordToDragonKeeper();
});

test.afterAll(async () => {
  await deleteSchema(api, route, SCHEMA);
  await api.dispose();
});

test.describe("when the dragonkeeper has permissions on the pet table only", () => {
  test("The dragonkeeper has the correct permissions", async ({ page }) => {
    await page.goto(route + SCHEMA_PATH + "/Pet");
    await expect(
      page.getByText("The requested page could not be found.")
    ).toBeVisible();
    await page.getByRole("button", { name: "Home" }).click();
    await expect(page).toHaveURL(route);

    await signin(page, USERNAME, PASSWORD);

    // check that other tables are not clickable
    await page.goto(route);
    await page.getByText(SCHEMA).click();
    await expect(page.getByText("Category")).toBeVisible();
    await expect(page.getByText("Order")).toBeVisible();
    await expect(page.getByText("User")).toBeVisible();

    await page.getByText("Pet", { exact: true }).click();
    await expect(page.getByText("No records found")).toBeVisible();

    await signout(page);
    await expect(page.getByLabel("error")).toBeVisible();

    await signin(page, "admin", "admin");
    await page.getByRole("searchbox", { name: "Search Pet" }).fill("pooky");
    await expect(
      page
        .locator("div")
        .filter({ hasText: /^pooky$/ })
        .first()
    ).toBeVisible();
  });
});

test.describe("when the dragonkeeper has also permissions on the order table", () => {
  test.beforeAll(async () => {
    await addRlsToTables();
  });

  test.afterAll(async () => {
    await removeRlsFromTables();
  });

  test("the dragonkeeper can now see the order table", async ({ page }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText(SCHEMA).click();
    await page.getByText("Order", { exact: true }).click();
    await expect(page.getByText("No records found")).toBeVisible();
  });

  test("the dragonkeeper can now see smaug in the pet table", async ({
    page,
  }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText(SCHEMA).click();
    await page.getByText("Pet", { exact: true }).click();
    await expect(
      page
        .locator("div")
        .filter({ hasText: /^smaug$/ })
        .first()
    ).toBeVisible();
  });

  test("the dragonkeeper can now add pets to the pet table", async ({
    page,
  }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText(SCHEMA).click();
    await page.getByText("Pet", { exact: true }).click();
    await page.getByRole("button", { name: "Add" }).click();
    await expect(page.getByLabel("Name")).toBeVisible();
  });
});

async function signin(page: Page, username: string, password: string) {
  await page.getByRole("button", { name: "Signin" }).click();
  await page.getByRole("textbox", { name: "Username" }).fill(username);
  await page.getByRole("textbox", { name: "Password" }).fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
  await timeout(200);
}

async function signout(page: Page) {
  await page.getByRole("button", { name: "Account" }).click();
  await expect(page.getByRole("button", { name: "Sign out" })).toBeVisible();
  await page.getByRole("button", { name: "Sign out" }).click();
  await timeout(200);
}

async function timeout(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

// every user inherits the anonymous role, so dragonkeeper reads all until this runs
async function dropAnonymousFromTestSchema() {
  return gql(
    api,
    `${route}${SCHEMA_PATH}/graphql`,
    `mutation drop($members: [String]) {
      drop(members: $members) {
        message
      }
    }`,
    { members: ["anonymous"] }
  );
}

async function addPasswordToDragonKeeper() {
  return gql(
    api,
    `${route}graphql`,
    `mutation{
      changePassword(email: "${USERNAME}", password: "${PASSWORD}"){
        status,message
      }
    }`
  );
}

async function addRlsToTables() {
  return gql(
    api,
    `${route}${SCHEMA_PATH}/graphql`,
    `mutation {
        change(
          roles: [
            {
              name: "DragonKeeper"
              permissions: [
                {
                  table: "Order"
                  select: true
                  insert: true
                  update: true
                  delete: true
                  isRowLevel: true
                }
                {
                  table: "Category"
                  select: true
                  insert: true
                  update: true
                  delete: true
                  isRowLevel: true
                }
              ]
            }
          ]
        ) {
          message
        }
      }`
  );
}

async function removeRlsFromTables() {
  return gql(
    api,
    `${route}${SCHEMA_PATH}/graphql`,
    `mutation {
        change(
          roles: [
            {
              name: "DragonKeeper"
              permissions: [
                {
                  table: "Order"
                  select: false
                  insert: false
                  update: false
                  delete: false
                  isRowLevel: false
                } 
                {
                  table: "Category"
                  select: false
                  insert: false
                  update: false
                  delete: false
                  isRowLevel: false
                }
              ]
            }
          ]
        ) {
          message
        }
      }`
  );
}
