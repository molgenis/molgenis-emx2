import { expect, test } from "@nuxt/test-utils/playwright";

let myBase: string;

test.beforeEach(async ({ context, baseURL }) => {
  myBase = baseURL as string;
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
  const resp = await goto("/all/collections", {
    waitUntil: "hydration",
  });
  console.log("Response status:", resp?.url());
  console.log("Base URL:", myBase);
  await expect(page.getByText("97 collections")).toBeVisible();
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("life");
  await expect(page.getByRole("main")).toContainText("18 collections");
  await page.locator("a").filter({ hasText: "Go to page 2" }).click();
  await expect(page.getByRole("main")).toContainText("18 collections");
});
