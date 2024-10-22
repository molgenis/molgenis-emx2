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

test("should show variables in menu if there are variables", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/all", { waitUntil: "hydration" });
  await expect(page.getByRole("navigation")).toContainText("Cohort studies");
  await expect(page.getByRole("navigation")).toContainText("Data sources");
  await expect(page.getByRole("navigation")).toContainText("Databanks");
  await expect(page.getByRole("navigation")).toContainText("Networks");
  await expect(page.getByRole("navigation")).toContainText("More");
  await page.getByRole("button", { name: "More" }).hover();
  await page.locator("li").filter({ hasText: "Variables" });
});
