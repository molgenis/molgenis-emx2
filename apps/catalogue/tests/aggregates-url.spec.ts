import { expect, test } from "@nuxt/test-utils/playwright";

test.describe("Aggregates URL", () => {
  test.beforeEach(async ({ context, baseURL, goto }) => {
    await context.addCookies([
      {
        name: "mg_allow_analytics",
        value: "false",
        domain: new URL(baseURL as string).hostname,
        path: "/",
      },
    ]);

    await goto("/FORCE-NEN/collections", { waitUntil: "hydration" });
  });

  test.afterEach(async ({ goto }) => {
    await goto("/FORCE-NEN/collections", { waitUntil: "hydration" });
  });

  test("Aggregates URLs are properly defined", async ({ page }) => {
    const targetHref = "/Aggregates/aggregates/#/";
    await expect(
      page.getByRole("link", { name: "Aggregates" }).first()
    ).toHaveAttribute("href", targetHref);
    await expect(
      page.getByRole("link", { name: "Aggregates" }).nth(1)
    ).toHaveAttribute("href", targetHref);
  });

  test("First link properly resolves to dashboard when clicked", async ({
    page,
  }) => {
    await page.getByRole("link", { name: "Aggregates" }).first().click();
    const aggPagePromise = page.waitForEvent("popup");
    const aggPage = await aggPagePromise;
    await expect(
      aggPage.getByRole("button", { name: "Apply Filters" })
    ).toBeVisible();
  });

  test("Second link properly resolves to dashboard when clicked", async ({
    page,
  }) => {
    await page.getByRole("link", { name: "Aggregates" }).nth(1).click();
    const aggPagePromise = page.waitForEvent("popup");
    const aggPage = await aggPagePromise;
    await expect(
      aggPage.getByRole("button", { name: "Apply Filters" })
    ).toBeVisible();
  });
});
