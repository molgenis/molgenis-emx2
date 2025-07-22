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
  await goto("/all", { waitUntil: "hydration" });
  await expect(page.getByRole("navigation")).toContainText("Collections");
  await expect(page.getByRole("navigation")).toContainText("Networks");
  await expect(page.getByRole("navigation")).toContainText("Variables");
  await expect(page.getByRole("navigation")).toContainText("More");
  await page.getByRole("button", { name: "More" }).hover();
  await page.locator("li").filter({ hasText: "Variables" });
});
