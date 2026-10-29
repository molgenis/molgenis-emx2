import type { Page } from "@playwright/test";
import { expect, test } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

async function signinOnLoginPage(page: Page) {
  await page.getByRole("textbox", { name: "Username" }).fill("admin");
  await page.getByRole("textbox", { name: "Password" }).fill("admin");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("banner")).toContainText("Account");
}

test("the auth middleware sends the user back to the page they came from", async ({
  page,
}) => {
  await page.goto(`${route}pet%20store/Category`);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(
    "Category"
  );

  await page.getByRole("button", { name: "Signin" }).click();

  await expect(page).toHaveURL(/\/login\?/);
  const query = new URL(page.url()).searchParams;
  expect(query.get("schema")).toBe("pet store");
  expect(decodeURIComponent(query.get("redirectTo") ?? "")).toBe(
    "/pet store/Category"
  );

  await signinOnLoginPage(page);

  await expect(page).toHaveURL(/\/pet%20store\/Category$/);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(
    "Category"
  );
});

const evilRedirects = [
  "https://evil.example/steal",
  "//evil.example/steal",
  "/\\evil.example/steal",
  "/\t/evil.example/steal",
  "javascript:alert(1)",
];

for (const redirectTo of evilRedirects) {
  test(`login does not follow the redirect ${JSON.stringify(
    redirectTo
  )}`, async ({ page }) => {
    const evilRequests: string[] = [];
    await page.route("**://evil.example/**", (request) => {
      evilRequests.push(request.request().url());
      return request.abort();
    });
    page.on("dialog", (dialog) => {
      throw new Error(`unexpected dialog: ${dialog.message()}`);
    });

    await page.goto(
      `${route}login?redirectTo=${encodeURIComponent(redirectTo)}`
    );
    const ownHost = new URL(page.url()).host;

    await signinOnLoginPage(page);

    // the rejected value stays behind in the query string, so only check
    // where we actually are, not the full url
    const current = new URL(page.url());
    expect(current.host).toBe(ownHost);
    expect(current.pathname).not.toContain("evil.example");
    expect(evilRequests).toEqual([]);
  });
}
