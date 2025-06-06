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

test("go back from details, filter should stil be active", async ({
  page,
  goto,
}) => {
  await goto("/testNetworkofNetworks/collections", {
    waitUntil: "hydration",
  });

  //TODO unclear what it should do exactly

  // await page
  //   .locator("div:nth-child(16) > .inline-flex > .rotate-180 > svg")
  //   .click();
  //await page.getByRole('complementary').getByText('Longitudinal').click();
  //todo demo data is missing designType
  //await page.getByRole('link', { name: 'acronym for test cohort 1' }).click();
  //await page.goBack()
  //await expect(page.getByRole('main')).toContainText('4 cohort studies');
  //await expect(page.getByLabel('Longitudinal')).toBeChecked();
});
