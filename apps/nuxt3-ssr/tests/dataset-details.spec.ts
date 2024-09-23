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

test("show dataset details on cohorts page", async ({ page, goto }) => {
  await goto("/catalogue-demo/ssr-catalogue/", { waitUntil: "hydration" });
  await page.getByRole("button", { name: "Reject" }).click();
  await page.getByRole("link", { name: "All resources" }).click();
  await page.getByRole("button", { name: "Cohort studies" }).click();
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("genr");
  await page.getByRole("button", { name: "Search", exact: true }).click();
  await page.getByRole("link", { name: "GenR", exact: true }).click();
  await page.getByRole("link", { name: "Networks" }).click();
  await page.getByRole("link", { name: "Datasets" }).click();
  await page.getByText("FETALCRL_22112016").click();
  await expect(
    page.getByText("DataWiki dataset FETALCRL_22112016")
  ).toBeVisible();
});
