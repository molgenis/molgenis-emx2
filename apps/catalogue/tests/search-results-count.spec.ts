/**
 * Test all instances where the component <SearchResultsCounts> is used.
 * All values must follow this format: <value-prefix> <value> <label>.
 * Value prefixes are optional and used if you would like to add a label
 * before the value (e.g., `Found 12 results`). If testing locally, disable
 * the `enableRejectCookiesClick` as this does not exist when running the
 * preview locally and the action will timeout.
 */

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

const numberOfResultsPattern = new RegExp(
  /^(([a-zA-Z]{1,})?(\s)?(([0-9]{1,})\s(cohort studie([s])?|variable([s])?|data\ssource([s])?|result([s])?|networks([s])?|collection([s])?)))$/
);

test("validate collection search result counts @collection-view @search-result-counts", async ({
  page,
  goto,
}) => {
  await goto("/all/collections", {
    waitUntil: "hydration",
  });

  const text = await page.locator(".search-results-count").textContent();
  await expect(text).toMatch(numberOfResultsPattern);
});

test("validate networks sources search result counts @networks-view @search-result-counts", async ({
  page,
  goto,
}) => {
  await goto("/all/networks", {
    waitUntil: "hydration",
  });

  const text = await page.locator(".search-results-count").textContent();
  await expect(text).toMatch(numberOfResultsPattern);
});

test("validate variables in cohorts counts are shown", async ({
  page,
  goto,
}) => {
  await goto("/all/variables", {
    waitUntil: "hydration",
  });

  await expect(page.getByRole("main")).toContainText("2249 variables");
});
