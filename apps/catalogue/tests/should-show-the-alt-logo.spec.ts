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

test("should show the alt logo", async ({ page, goto }) => {
  await goto("/?logo=UMCGkort.woordbeeld", {
    waitUntil: "hydration",
  });
  await expect(page).toHaveScreenshot({
    clip: { x: 0, y: 0, width: 200, height: 100 },
    threshold: 0.4,
  });
});
