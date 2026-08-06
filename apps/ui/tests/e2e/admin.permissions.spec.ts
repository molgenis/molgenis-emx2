import { test, expect, request as apiRequest } from "@playwright/test";
import type { APIRequestContext } from "@playwright/test";
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

test("test admin page is shown after login and page refresh", async ({
  page,
}) => {
  await page.goto(`${route}admin`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(
    "Admin Tools"
  );
});
