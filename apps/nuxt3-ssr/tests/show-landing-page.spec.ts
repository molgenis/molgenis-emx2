import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("test", async ({ page, goto }) => {
  await goto("/catalogue-demo/ssr-catalogue/", { waitUntil: "hydration" });
  await expect(
    page.getByRole("heading", {
      name: "European Health Research Data and Sample Catalogue",
    })
  ).toBeVisible();
});
