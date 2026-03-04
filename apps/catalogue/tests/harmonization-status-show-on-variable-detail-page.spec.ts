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

test("test hamonisation status is show in varaible on variable detail page", async ({
  page,
  goto,
}) => {
  await goto("/", { waitUntil: "hydration" });
  await page
    .getByRole("row", { name: "ATHLETE Advancing Tools for" })
    .getByRole("button")
    .click();
  await page.getByRole("button", { name: "Variables" }).click();
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("fetus_abd_circum_t");
  await page
    .getByRole("link", { name: "fetus_abd_circum_t", exact: true })
    .click();
  //todo check with data manager await expect(page.getByRole('row', { name: 'fetus_abd_circum_t partial' }).getByRole('img')).toBeVisible();
});
