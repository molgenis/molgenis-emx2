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

test("show dataset details on cohorts page", async ({ page, goto }) => {
  await goto("/catalogue-demo/catalogue/", { waitUntil: "hydration" });
  await page.getByRole("link", { name: "All collections" }).click();
  // todo temporary fix for the issue with the search bar being cleared by page reload due to async data fetch
  await page.waitForTimeout(3000);
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("genr");
  await page.getByRole("link", { name: "GenR", exact: true }).click();
  await page.getByRole("link", { name: "Datasets" }).click();
  await page.getByText("FETALCRL_22112016").click();
  await expect(
    page.getByText("DataWiki dataset FETALCRL_22112016")
  ).toBeVisible();
});
