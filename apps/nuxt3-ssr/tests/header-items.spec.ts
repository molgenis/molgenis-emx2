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

test("should show variables in menu if there are variables", async ({
  page,
  goto,
}) => {
  await goto("/catalogue-demo/ssr-catalogue/all", { waitUntil: "hydration" });
  await expect(page.getByRole("navigation")).toContainText("Variables");
});
