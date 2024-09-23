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

test("offset should be reset ( back to page 1) when filter changes ", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/EUChildNetwork/variables", {
    waitUntil: "hydration",
  });
  await expect(page.locator("text=abd_circum_sdsWHO_t").first()).toBeVisible();
  await page.locator("a").filter({ hasText: "Go to page 2" }).click();
  //todo, why doesn't this work? await expect(page.locator('text=aggr_pc_').first()).toBeVisible();
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("food");
  await expect(page.locator("text=allergy_food_m").first()).toBeVisible();
});
