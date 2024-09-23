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

test("should not show about menu item on non catalogue spesific page", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/all", { waitUntil: "hydration" });
  await expect(page.getByRole("link", { name: "About" })).toHaveCount(0);
});

test("should show about menu item on catalogue spesific page", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/IPEC", { waitUntil: "hydration" });
  await expect(page.getByRole("link", { name: "About" })).toHaveCount(1);
});
