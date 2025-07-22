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

test("filter should remain active after page (pagination) change ", async ({
  page,
  goto,
}) => {
  await goto("/all/collections", {
    waitUntil: "hydration",
  });
  await expect(page.getByRole("main")).toContainText("96 collections");
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("life");
  await expect(page.getByRole("main")).toContainText("21 collections");
  await page.locator("a").filter({ hasText: "Go to page 2" }).click();
  await expect(page.getByRole("main")).toContainText("21 collections");
});
