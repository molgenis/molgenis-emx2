import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

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

test("filter cohorts list page by design", async ({ page }) => {
  await page.goto(
    "/catalogue-demo/ssr-catalogue/all/cohorts?page=1&conditions=[{%22id%22:%22cohortDesigns%22,%22conditions%22:[{%22name%22:%22Cross-sectional%22}]}]"
  );
  await expect(page.getByRole("main")).toContainText("Cross-sectional");
  await page.getByRole("button", { name: "Design -" }).click();
  await expect(page.getByRole("complementary")).toContainText("Longitudinal");
});
