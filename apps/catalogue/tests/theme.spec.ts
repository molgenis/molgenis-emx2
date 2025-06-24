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

test("landing-page-molgenis", async ({ page, goto }) => {
  await goto("/", {
    waitUntil: "hydration",
  });
  await expect(page).toHaveScreenshot({
    fullPage: true,
    maxDiffPixelRatio: 0.4,
  });
});

test("landing-page-umcg", async ({ page, goto }) => {
  await goto("/?theme=umcg", {
    waitUntil: "hydration",
  });
  await expect(page).toHaveScreenshot({
    fullPage: true,
    maxDiffPixelRatio: 0.4,
  });
});

test("landing-page-aumc", async ({ page, goto }) => {
  await goto("/?theme=aumc", {
    waitUntil: "hydration",
  });
  await expect(page).toHaveScreenshot({
    fullPage: true,
    maxDiffPixelRatio: 0.4,
  });
});
