import { expect, test } from "@nuxt/test-utils/playwright";

test("test", async ({ page, goto }) => {
  await goto("/", { waitUntil: "hydration" });
  await expect(
    page.getByRole("heading", {
      name: "European Health Research Data and Sample Catalogue",
    })
  ).toBeVisible();
});
