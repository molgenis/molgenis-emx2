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

test("network detail resource listing resource detail should show the resoource details page", async ({
  page,
  goto,
}) => {
  await goto("/testNetwork1/collections", {
    waitUntil: "hydration",
  });
  await page.getByText("acronym for test cohort 1").click();
  //await page.getByRole('button', { name: 'Detail page' }).click();
  await expect(page.locator("h1")).toContainText("acronym for test cohort 1");
});
