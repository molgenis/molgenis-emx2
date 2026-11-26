import { expect, test } from "@nuxt/test-utils/playwright";

// Taller than the tables page content, so there is space left over below the content.
test.use({ viewport: { width: 1280, height: 1600 } });

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

test("footer sticks to the bottom when content is shorter than the screen", async ({
  page,
  goto,
}) => {
  await goto("/tables", { waitUntil: "hydration" });

  const footer = page
    .locator("div.bg-footer")
    .filter({ hasText: "Created with MOLGENIS" });
  await expect(footer).toBeVisible();
  // wait for the version line, it changes the footer height when it loads
  await expect(page.getByText(/Software version/)).toBeVisible();

  const box = await footer.boundingBox();
  const viewportHeight = page.viewportSize()!.height;
  expect(box!.y + box!.height).toBeCloseTo(viewportHeight, 0);

  await expect(page).toHaveScreenshot("tables-short-content.png", {
    // the footer is painted as a solid block, so its position is what gets compared;
    // the header (logo) and headings depend on the catalogue data, so they are masked too
    mask: [footer, page.locator("header"), page.getByRole("heading")],
    maxDiffPixelRatio: 0.05,
  });
});
