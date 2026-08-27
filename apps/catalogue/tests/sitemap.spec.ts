import { test, expect } from "@nuxt/test-utils/playwright";

test("test sitemap generation", async ({ request }) => {
  const resp = await request.get(
    "http://localhost:8080/catalogue-demo/sitemap.xml"
  );
  const xml = await resp.text();
  await expect(xml).toContain(
    `<loc>https://localhost:8080/all/collections/ABCD</loc>`
  );
});
