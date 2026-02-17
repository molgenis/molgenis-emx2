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

test("catalogue query param appears in collection links on the collections page", async ({
  page,
  goto,
}) => {
  await goto("/testNetwork1/collections", { waitUntil: "hydration" });

  // Links to individual collections should carry ?catalogue=testNetwork1
  const collectionLink = page
    .getByRole("link", { name: "acronym for test cohort 1" })
    .first();
  await expect(collectionLink).toBeVisible();
  const href = await collectionLink.getAttribute("href");
  expect(href).toContain("catalogue=testNetwork1");
});

test("catalogue query param persists when navigating to a collection detail", async ({
  page,
  goto,
}) => {
  await goto("/testNetwork1/collections", { waitUntil: "hydration" });
  await page.getByText("acronym for test cohort 1").click();
  await expect(page.locator("h1")).toContainText("acronym for test cohort 1");

  // URL should still carry the catalogue param
  const url = new URL(page.url());
  expect(url.searchParams.get("catalogue")).toBe("testNetwork1");
});
