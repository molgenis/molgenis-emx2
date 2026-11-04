import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

test.skip(
  !!process.env.CI,
  "Skipping in CI, due to inability to handle multiple pages in current test setup"
);
test("Re-authentication flow", async ({ page }) => {
  await page.goto(`${route}pet%20store/Category`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(
    "Category"
  );

  // signin
  await page.goto(`${route}pet%20store/Category`);
  await page.getByRole("button", { name: "Signin" }).click();
  await page.getByRole("textbox", { name: "Username" }).click();
  await page.getByRole("textbox", { name: "Username" }).fill("admin");
  await page.getByRole("textbox", { name: "Username" }).press("Tab");
  await page.getByRole("textbox", { name: "Password" }).fill("admin");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByRole("button", { name: "Add Category" }).click();
  await page.getByRole("textbox", { name: "name Required" }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill("reauth");

  const pageTwoPromise = await page.context().newPage();
  const pageTwo = await pageTwoPromise;
  await pageTwo.goto(`${route}/pet%20store/Category`);

  await pageTwo.getByRole("button", { name: "Account" }).click();
  await pageTwo.getByRole("button", { name: "Sign out" }).click();

  await page.getByRole("textbox", { name: "name Required" }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByText("Your session has expired.")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Re-authenticate" })
  ).toBeVisible();

  const reAuthenticatePagePromise = page.waitForEvent("popup");
  await page.getByRole("button", { name: "Re-authenticate" }).click();
  const reAuthenticatePage = await reAuthenticatePagePromise;
  await reAuthenticatePage
    .getByRole("textbox", { name: "Username" })
    .fill("admin");
  await reAuthenticatePage
    .getByRole("textbox", { name: "Username" })
    .press("Tab");
  await reAuthenticatePage
    .getByRole("textbox", { name: "Password" })
    .fill("admin");
  await reAuthenticatePage.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByText("Re-authenticated, please")).toBeVisible();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  // wait for the insert to finish, closing the modal earlier skips the table refresh
  await expect(page.getByText("inserted Category")).toBeVisible();
  await page.getByRole("button", { name: "Close modal", exact: true }).click();
  await expect(page.getByText("reauth", { exact: true })).toBeVisible();

  // cleanup, so a next run can insert the row again
  await page.getByRole("searchbox", { name: "Search Category" }).fill("reauth");
  await page.getByRole("row").nth(1).hover();
  await page.getByRole("button", { name: 'delete {"name":"reauth"}' }).click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "reauth", exact: true })
  ).not.toBeVisible();
});
