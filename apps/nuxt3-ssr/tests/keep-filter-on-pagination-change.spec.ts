import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("filter should remain active after page (pagination) change ", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/all/cohorts", {
    waitUntil: "hydration",
  });
  await page.getByRole("button", { name: "Accept" }).click();
  await expect(page.getByRole("main")).toContainText("57 cohort studies");
  await page.getByPlaceholder("Type to search..").click();
  await page.getByPlaceholder("Type to search..").fill("life");
  await expect(page.getByRole("main")).toContainText("19 cohort studies");
  await page.locator("a").filter({ hasText: "Go to page 2" }).click();
  await expect(page.getByRole("main")).toContainText("19 cohort studies");
});
