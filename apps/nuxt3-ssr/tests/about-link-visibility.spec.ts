import { expect, test } from "@nuxt/test-utils/playwright";
import { fileURLToPath } from "node:url";

test.use({
  nuxt: {
    rootDir: fileURLToPath(new URL("..", import.meta.url)),
    host: process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/",
  },
});

test("should not show about menu item on non catalogue spesific page", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/all", { waitUntil: "hydration" });
  await page.getByRole("button", { name: "Accept" }).click();
  await expect(page.getByRole("link", { name: "About" })).toHaveCount(0);
});

test("should show about menu item on catalogue spesific page", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/IPEC", { waitUntil: "hydration" });
  await page.getByRole("button", { name: "Accept" }).click();
  await expect(page.getByRole("link", { name: "About" })).toHaveCount(1);
});
