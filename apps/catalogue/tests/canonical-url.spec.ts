import { test, expect } from "@nuxt/test-utils/playwright";

test.describe("canonical URL tags", () => {
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

  test("landing page has a canonical link tag", async ({ page, goto }) => {
    await goto("/", { waitUntil: "hydration" });
    const canonical = page.locator('link[rel="canonical"]');
    await expect(canonical).toHaveAttribute("href", /\/$/);
  });

  test("collection detail page under /all/ has a canonical link tag pointing to itself", async ({
    page,
    goto,
  }) => {
    await goto("/all/collections/ABCD", { waitUntil: "hydration" });
    const canonical = page.locator('link[rel="canonical"]');
    await expect(canonical).toHaveAttribute(
      "href",
      /\/all\/collections\/ABCD$/
    );
  });
});
