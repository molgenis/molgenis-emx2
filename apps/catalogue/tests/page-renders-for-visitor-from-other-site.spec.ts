import { expect, test } from "@nuxt/test-utils/playwright";

test("page renders for a visitor who comes from another site", async ({
  goto,
}) => {
  const response = await goto("/all", {
    referer: "https://www.google.com/",
    waitUntil: "hydration",
  });
  expect(response?.status()).toBe(200);
});
