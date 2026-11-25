import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

// Taller than the login page content, so there is space left over below the content.
test.use({ viewport: { width: 1280, height: 1600 } });

test("footer sticks to the bottom when content is shorter than the screen", async ({
  page,
}) => {
  await page.goto(`${route}login`);
  await expect(page.getByRole("heading", { name: "Login" })).toBeVisible();

  const footer = page
    .locator("div.bg-footer")
    .filter({ hasText: "Created with MOLGENIS" });
  await expect(footer).toBeVisible();
  // wait for the version line, it changes the footer height when it loads
  await expect(page.getByText(/Software version/)).toBeVisible();

  const box = await footer.boundingBox();
  const viewportHeight = page.viewportSize()!.height;
  expect(box!.y + box!.height).toBeCloseTo(viewportHeight, 0);

  await expect(page).toHaveScreenshot("login-short-content.png", {
    // the footer is painted as a solid block, so its position is what gets compared
    mask: [footer],
    maxDiffPixelRatio: 0.05,
  });
});
