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

test("catalogue description should be shown", async ({ page, goto }) => {
  await goto("/catalogue-demo/ssr-catalogue/all", { waitUntil: "hydration" });
  await page.getByRole("button", { name: "Accept" }).click();
  await expect(page.getByRole("main")).toContainText(
    "Select one of the content categories listed below."
  );
});
