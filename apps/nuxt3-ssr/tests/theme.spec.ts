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
  await goto("/catalogue-demo/ssr-catalogue/", {
    waitUntil: "hydration",
  });
  await expect(page).toHaveScreenshot({
    fullPage: true,
    maxDiffPixels: 500,
  });
});

test("landing-page-umcg", async ({ page, goto }) => {
  await goto("/catalogue-demo/ssr-catalogue/?theme=umcg", {
    waitUntil: "hydration",
  });
  await expect(page).toHaveScreenshot({
    fullPage: true,
    maxDiffPixels: 500,
  });
});

test("landing-page-aumc", async ({ page, goto }) => {
  await goto("/catalogue-demo/ssr-catalogue/?theme=aumc", {
    waitUntil: "hydration",
  });
  await expect(page).toHaveScreenshot({
    fullPage: true,
    maxDiffPixels: 500,
  });
});
