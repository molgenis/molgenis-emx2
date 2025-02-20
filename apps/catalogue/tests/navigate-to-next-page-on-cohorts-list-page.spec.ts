import { expect, test } from "@nuxt/test-utils/playwright";

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([
    {
      name: "mg_allow_analytics",
      value: "false",
      domain: new URL(baseURL as string).hostname,
      path: "/",
    },
  ]);
});

test("navigate-to-next-page-on-cohorts-list-page", async ({ page, goto }) => {
  await goto("/catalogue-demo/catalogue/ATHLETE", {
    waitUntil: "hydration",
  });
  await page.getByRole("button", { name: "Collections" }).click();
  await page.locator("a").filter({ hasText: "Go to page 2" }).click();
  await expect(page.getByRole("main")).toContainText("SEPAGES");
});
