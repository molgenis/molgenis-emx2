import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: process.env.E2E_BASE_URL
      ? undefined
      : fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("test hamonisation status is show in varaible on variable detail page", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/", { waitUntil: "hydration" });
  await page.getByRole("button", { name: "Accept" }).click();
  await page.getByText("ATHLETE").click();
  await page.getByRole("button", { name: "Variables" }).click();
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("fetus_abd_circum_t");
  await page
    .getByRole("link", { name: "fetus_abd_circum_t", exact: true })
    .click();
  //todo check with data manager await expect(page.getByRole('row', { name: 'fetus_abd_circum_t partial' }).getByRole('img')).toBeVisible();
});
