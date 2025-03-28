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

test("filter down resoure variables", async ({ page, goto }) => {
  await goto("/catalogue-demo/catalogue/LongITools/collections/ENVIRONAGE", {
    waitUntil: "hydration",
  });
  await page
    .getByRole("complementary")
    .getByRole("link", { name: "Variables" })
    .click();
  await page
    .locator('select[name="filter-by-data-set"]')
    .selectOption("bloodpressure after birth");
  await page.locator('input[name="filter-by-variable"]').click();
  await page.locator('input[name="filter-by-variable"]').fill("height");
  await page.getByRole("combobox").first().selectOption("measurements at FU1");
  await page.getByText("Height").click();
  await page.getByRole("button", { name: "Detail page" }).click();
  await expect(page.getByText("Height of child in")).toBeVisible();
});
