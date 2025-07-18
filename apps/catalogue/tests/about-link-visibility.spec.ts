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

test("should not show about menu item on non catalogue specific page", async ({
  page,
  goto,
}) => {
  await goto("/all", { waitUntil: "hydration" });
  await expect(
    page.getByRole("link", { name: "About", exact: true })
  ).toHaveCount(0);
});

test("should show about menu item on catalogue specific page", async ({
  page,
  goto,
}) => {
  await goto("/IPEC", { waitUntil: "hydration" });
  await expect(
    page.getByRole("link", { name: "About", exact: true })
  ).toHaveCount(1);
});
