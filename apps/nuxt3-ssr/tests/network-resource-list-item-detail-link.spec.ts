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

test("network detail resource listing resource detail should show the resoource details page", async ({
  page,
}) => {
  await page.goto("/catalogue-demo/ssr-catalogue/testNetwork1/cohorts");
  await page.getByText("acronym for test cohort 1").click();
  //await page.getByRole('button', { name: 'Detail page' }).click();
  await expect(page.locator("h1")).toContainText("acronym for test cohort 1");
});
