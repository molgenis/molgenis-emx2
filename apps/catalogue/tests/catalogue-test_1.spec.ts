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

test("Catalogue test number 1: Athlete network manager", async ({
  page,
  goto,
}) => {
  await goto("/", { waitUntil: "hydration" });

  await expect(page.locator("h1")).toContainText(
    "European Health Research Data and Sample Catalogue"
  );
  await expect(page.getByRole("main")).toContainText("Project catalogues");
  await expect(page.getByRole("main")).toContainText("ATHLETE");
  await expect(page.getByRole("main")).toContainText(
    "Advancing Tools for Human Early Lifecourse Exposome Research and Translation"
  );
  await page
    .getByRole("row", { name: "ATHLETE Advancing Tools for" })
    .getByRole("button")
    .click();
  await expect(page.getByRole("main")).toContainText("ATHLETE");
  await expect(page.getByRole("main")).toContainText("Collections");
  await expect(page.getByRole("main")).toContainText("Variables");
});
