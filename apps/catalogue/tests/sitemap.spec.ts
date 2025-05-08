import { test, expect } from "@nuxt/test-utils/playwright";

test("test sitemap generation", async ({ request }) => {
  const location = process.env.CI
    ? process.env.E2E_BASE_URL
    : "http://localhost:8080/";
  const resp = await request.get(location + "/catalogue/sitemap.xml");
  const xml = await resp.text();
  expect(xml).toContain(
    `<loc>${location}catalogue/catalogue/all/collections/ABCD</loc>`
  );
});
