import { APIRequestContext, Page, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

export async function findAndDeleteRow(
  page: Page,
  tableName: string,
  rowName: string
) {
  await page
    .getByRole("searchbox", { name: `Search ${tableName}` })
    .fill(rowName);
  await expect(
    page.locator("div").filter({ hasText: rowName }).first()
  ).toBeVisible();
  await page.getByRole("cell", { name: rowName }).hover();
  await page
    .getByRole("button", { name: `delete {"name":"${rowName}"}` })
    .click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: `view row details ${rowName}` })
  ).toBeHidden();
}

export async function insertRow(page: Page, tableName: string) {
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByText(`inserted ${tableName}`).click();
  await page.getByRole("button", { name: "Cancel" }).click();
}

export async function signin(page: Page, username: string, password: string) {
  await page.getByRole("button", { name: "Signin" }).click();
  await page.getByRole("textbox", { name: "Username" }).fill(username);
  await page.getByRole("textbox", { name: "Password" }).fill(password);
  await page.getByRole("button", { name: "Sign in" }).click();
  await timeout(200);
}

export async function signout(page: Page) {
  await page.getByRole("button", { name: "Account" }).click();
  await expect(page.getByRole("button", { name: "Sign out" })).toBeVisible();
  await page.getByRole("button", { name: "Sign out" }).click();
  await timeout(200);
}

export async function timeout(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function gql(
  api: APIRequestContext,
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

export async function becomeAdmin(api: APIRequestContext) {
  return gql(
    api,
    `${route}graphql`,
    `mutation {
      signin(email: "admin", password: "admin") {
        status
        message
      }
    }`
  );
}

export async function dropAnonymousFromPetStore(api: APIRequestContext) {
  return gql(
    api,
    `${route}pet%20store/graphql`,
    `mutation drop($members: [String]) {
      drop(members: $members) {
        message
      }
    }`,
    { members: ["anonymous"] }
  );
}

export async function addPasswordToUser(
  api: APIRequestContext,
  userName: string,
  password: string
) {
  return gql(
    api,
    `${route}graphql`,
    `mutation{
      changePassword(email: "${userName}", password: "${password}"){
        status,message
      }
    }`
  );
}

export async function restoreAnonymousToPetStore(api: APIRequestContext) {
  return gql(
    api,
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

export async function addRlsToTables(api: APIRequestContext) {
  return gql(
    api,
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

export async function removeRlsFromTables(api: APIRequestContext) {
  return gql(
    api,
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
