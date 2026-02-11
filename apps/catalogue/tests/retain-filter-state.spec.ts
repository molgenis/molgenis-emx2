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

test("go back from details, filter should stil be active", async ({
  page,
  goto,
}) => {
  await goto("/testNetworkofNetworks/collections", {
    waitUntil: "hydration",
  });
});
