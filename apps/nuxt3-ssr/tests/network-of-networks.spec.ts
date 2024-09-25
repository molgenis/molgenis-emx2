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

test("show network of networks", async ({ page, goto }) => {
  await goto("/catalogue-demo/ssr-catalogue/testNetworkofNetworks", {
    waitUntil: "hydration",
  });
  await expect(page.getByText("7", { exact: true })).toBeVisible();
  await expect(page.getByText("4", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Cohort studies" }).click();
  await expect(page.getByText("4 cohort studies")).toBeVisible();
  await goto("/catalogue-demo/ssr-catalogue/testNetworkofNetworks", {
    waitUntil: "hydration",
  });
  await page.getByRole("button", { name: "Variables" }).click();
  await expect(page.getByText("7 variables")).toBeVisible();
  //todo check if not should be 9
  await page.getByRole("heading", { name: "Resources" }).click();
  await page.getByRole("heading", { name: "Resources" }).click();
  await expect(page.getByText("testCohort4")).toBeVisible();
  await page.getByRole("button", { name: "Harmonisations" }).click();
  await expect(
    page.getByRole("cell", { name: "testCohort4" }).locator("span")
  ).toBeVisible();
});
