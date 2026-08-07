import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext, Page } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

// shared context so the signin cookie is reused by the follow-up mutations
let api: APIRequestContext;

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

test.beforeAll(async () => {
  api = await apiRequest.newContext();

  await gql(
    `${route}graphql`,
    `mutation {
      signin(email: "admin", password: "admin") {
        status
        message
      }
    }`
  );

  await gql(
    `${route}pet%20store/graphql`,
    `mutation drop($members: [String]) {
      drop(members: $members) {
        message
      }
    }`,
    { members: ["anonymous"] }
  );
});

test.afterAll(async () => {
  await gql(
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
        enabled: true,
        roles: [{ schemaId: "pet store", role: "Viewer" }],
      },
    }
  );

  await api.dispose();
});

test("The dragonkeeper has the correct permissions", async ({ page }) => {
  await page.goto(route + "pet%20store/Pet");
  await expect(
    page.getByText("The requested page could not be found.")
  ).toBeVisible();
  await page.getByRole("button", { name: "Home" }).click();
  await expect(page).toHaveURL(route);

  await signin(page, "dragonKeeper", "dragonKeeper");

  // check that order tables are not clickable
  await page.goto(route + "pet%20store");
  expect(page.getByText("Category")).toBeVisible();
  expect(page.getByText("Order")).toBeVisible();
  expect(page.getByText("User")).toBeVisible();

  await page.getByText("pet store").click();
  await page.getByText("Pet", { exact: true }).click();
  expect(page.getByText("No records found")).toBeVisible();

  await page.getByText("Pet", { exact: true }).click();
  await page.getByRole("button", { name: "Account" }).click();
  await page.getByRole("button", { name: "Sign out" }).click();
  expect(page.getByLabel("error")).toHaveText(
    "You don't have permission to view this table. Please contact your administrator to request access."
  );

  signin(page, "admin", "admin");
  expect(page.getByText("Showing 1 to 10 of 10 items")).toBeVisible();
});

async function signin(page: Page, username: string, password: string) {
  return page
    .getByRole("button", { name: "Signin" })
    .click()
    .then(() => page.getByRole("textbox", { name: "Username" }).fill(username))
    .then(() => page.getByRole("textbox", { name: "Password" }).fill(password))
    .then(() => page.getByRole("button", { name: "Sign in" }).click());
}
