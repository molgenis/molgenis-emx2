import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext, Page } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";
// test.use({ storageState: "playwright/.auth/user.json" });

// shared context so the signin cookie is reused by the follow-up mutations
let api: APIRequestContext;
const USERNAME = "dragonkeeper";
const PASSWORD = "dragonkeeper";

test.describe.configure({ mode: "serial" });

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await becomeAdmin();
  await dropAnonymousFromPetStore();
  await addPasswordToDragonKeeper();
});

test.afterAll(async () => {
  await restoreAnonymousToPetStore();
  await api.dispose();
});

test.describe("when the dragonkeeper has permissions on the pet table only", () => {
  test("The dragonkeeper has the correct permissions", async ({ page }) => {
    await page.goto(route + "pet%20store/Pet");
    await expect(
      page.getByText("The requested page could not be found.")
    ).toBeVisible();
    await page.getByRole("button", { name: "Home" }).click();
    await expect(page).toHaveURL(route);

    await signin(page, USERNAME, PASSWORD);

    // check that other tables are not clickable
    await page.goto(route);
    await page.getByText("pet store").click();
    await expect(page.getByText("Category")).toBeVisible();
    await expect(page.getByText("Order")).toBeVisible();
    await expect(page.getByText("User")).toBeVisible();

    await page.getByText("Pet", { exact: true }).click();
    await expect(page.getByText("No records found")).toBeVisible();

    await signout(page);
    await expect(page.getByLabel("error")).toBeVisible();

    await signin(page, "admin", "admin");
    await expect(page.getByText("Showing 1 to 10 of 10 items")).toBeVisible();
  });
});

test.describe("when the dragonkeeper has also permissions on the order table", () => {
  test.beforeAll(async () => {
    await addRlsToOrderTable();
  });

  test.afterAll(async () => {
    await removeRlsFromOrderTable();
  });

  test("the dragonkeeper can now see the order table", async ({ page }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText("pet store").click();
    await page.getByText("Order", { exact: true }).click();
    await expect(page.getByText("No records found")).toBeVisible();
  });

  test("the dragonkeeper can now see smaug in the pet table", async ({
    page,
  }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText("pet store").click();
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
    await page.getByText("pet store").click();
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

async function gql(
  url: string,
  query: string,
  variables?: Record<string, unknown>
) {
  const response = await api.post(url, {
    headers: { "Content-Type": "application/json" },
    data: variables ? { query, variables } : { query },
  });
  const body = await response.json();
  if (body.errors) {
    throw new Error(`GraphQL error on ${url}: ${JSON.stringify(body.errors)}`);
  }
  return body.data;
}

async function becomeAdmin() {
  return gql(
    `${route}graphql`,
    `mutation {
      signin(email: "admin", password: "admin") {
        status
        message
      }
    }`
  );
}

async function dropAnonymousFromPetStore() {
  return gql(
    `${route}pet%20store/graphql`,
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
    `${route}graphql`,
    `mutation{
      changePassword(email: "${USERNAME}", password: "${PASSWORD}"){
        status,message
      }
    }`
  );
}

async function restoreAnonymousToPetStore() {
  return gql(
    `${route}graphql`,
    `mutation updateUser($updateUser: InputUpdateUser) {
      updateUser(updateUser: $updateUser) {
        status
        message
      }
    }`,
    {
      updateUser: {
        email: "anonymous",
        roles: [{ schemaId: "pet store", role: "Viewer" }],
      },
    }
  );
}

async function addRlsToOrderTable() {
  return gql(
    `${route}pet%20store/graphql`,
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

async function removeRlsFromOrderTable() {
  return gql(
    `${route}pet%20store/graphql`,
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
