import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

test.beforeAll(async () => {
  const query = `
    {
      "query":"mutation { drop(members: ["anonymous"]) { message } }",
    }`;
  const response = await fetch(`${route}pet%20store/graphql`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: query,
  }).catch((error) => {
    console.error("Error fetching base URL:", error);
  });
  console.log("Response from beforeAll fetch:", response);
});

test.afterAll(async () => {
  const query = `{
  "query": "mutation updateUser($updateUser: InputUpdateUser) { updateUser(updateUser: $updateUser) { status message } }",
  "variables": {
    "updateUser": {
      "email": "anonymous",
      "enabled": true,
      "revokedRoles": []
      "roles": [
        { "schemaId": "pet store", "role": "Viewer" }
      ]
    }
   }
  }`;
  const response = await fetch(`${route}graphql`, {
    method: "POST",
    body: query,
  }).catch((error) => {
    console.error("Error fetching base URL:", error);
  });
  console.log("Response from afterAll fetch:", response);
});

// test.beforeEach(async ({ page }) => {
//   await page.goto(`${route}pet%20store/Pet`);
//   if (await page.getByRole("button", { name: "Signin" })) {
//     await page.getByRole("button", { name: "Signin" }).click();
//     await page.getByRole("textbox", { name: "Username" }).click();
//     await page.getByRole("textbox", { name: "Username" }).fill("admin");
//     await page.getByRole("textbox", { name: "Username" }).press("Tab");
//     await page.getByRole("textbox", { name: "Password" }).fill("admin");
//     await page.getByRole("button", { name: "Sign in" }).click();
//   }

//   await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
// });

// test.afterEach(async ({ page }) => {
//   await page.getByRole("button", { name: "Account" }).click();
//   await page.getByRole("button", { name: "Sign out" }).click();
// });

test("test admin page is shown after login and page refresh", async ({
  page,
}) => {
  await page.goto(`${route}admin`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(
    "Admin Tools"
  );
});
