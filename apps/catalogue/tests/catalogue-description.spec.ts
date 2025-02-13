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

test("catalogue description should be shown", async ({ page, goto }) => {
  await goto("/catalogue-demo/catalogue/all", { waitUntil: "hydration" });
  await expect(page.getByRole("main")).toContainText(
    "Select one of the content categories listed below."
  );
});
