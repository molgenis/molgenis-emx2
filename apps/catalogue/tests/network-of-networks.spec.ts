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
  await goto("/testNetworkofNetworks", {
    waitUntil: "hydration",
  });
  await expect(page.getByText("8", { exact: true })).toBeVisible();
  await expect(page.getByText("2", { exact: true })).toBeVisible();
  await expect(page.getByText("2", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Collections" }).click();
  await expect(page.getByText("8 collections")).toBeVisible();
  await goto("/testNetworkofNetworks", {
    waitUntil: "hydration",
  });
  await page.getByRole("button", { name: "Variables" }).click();
  await expect(page.getByText("7 variables")).toBeVisible();
  await page.getByRole("checkbox", { name: "testCohort4" }).check();
  await page.getByRole("button", { name: "Harmonisations" }).click();
  await expect(page.getByRole("table").getByText("testCohort4")).toBeVisible();
});
