import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("Catalogue test number 1: Athlete network manager", async ({
  page,
  goto,
}) => {
  await goto("catalogue-demo/ssr-catalogue/", { waitUntil: "hydration" });
  await page.getByRole("button", { name: "Accept" }).click();
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
  await expect(page.getByRole("main")).toContainText("Cohorts");
  await expect(page.getByRole("main")).toContainText("Variables");
});
