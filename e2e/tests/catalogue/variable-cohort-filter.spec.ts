import { test, expect } from "@playwright/test";

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([
    {
      name: "mg_allow_analytics",
      value: "false",
      domain: new URL(baseURL as string).hostname,
      path: "/"
    }
  ]);
});

test("filter variables by cohort", async ({ page }) => {
  await page.goto("/catalogue-demo/ssr-catalogue/testNetwork1/variables");
  await expect(page.getByRole("list")).toContainText("testVarCategorical_");
  await expect(page.getByRole("list")).toContainText("testVarNoRepeats");
  await expect(page.getByRole("list")).toContainText("testVarRepeats_");
  await page.getByRole("complementary").getByRole("img").nth(2).click();
  await page.getByText("testCohort1").click();
  await expect(page.getByRole("main")).toContainText("testVarRepeats_");
  await page.getByRole("button", { name: "Harmonisations" }).click();
  await expect(page.locator("tbody")).toContainText("testVarNoRepeats");
  await expect(page.locator("tbody")).toContainText("testVarRepeats_");
  await page.getByText("Remove 1 selected").click();
  await expect(page.locator("thead")).toContainText("testCohort4");
});
