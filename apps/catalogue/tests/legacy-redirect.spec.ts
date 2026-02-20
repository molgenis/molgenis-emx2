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

test("old-style /network/collections/resource URL redirects to flat route", async ({
  page,
  goto,
}) => {
  await goto("/testNetwork1/collections/testCohort1", {
    waitUntil: "hydration",
  });

  // Should have been redirected to the flat route with catalogue query param
  const url = new URL(page.url());
  expect(url.pathname).toBe("/testCohort1");
  expect(url.searchParams.get("catalogue")).toBe("testNetwork1");

  // The collection detail page should render
  await expect(page.locator("h1")).toContainText("acronym for test cohort 1");
});

test("old-style /network/networks/resource URL redirects to flat route", async ({
  page,
  goto,
}) => {
  await goto("/testNetworkofNetworks/networks/testNetwork1", {
    waitUntil: "hydration",
  });

  const url = new URL(page.url());
  expect(url.pathname).toBe("/testNetwork1");
  expect(url.searchParams.get("catalogue")).toBe("testNetworkofNetworks");
});
