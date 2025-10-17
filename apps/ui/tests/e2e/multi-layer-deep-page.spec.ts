import { test, expect } from "@playwright/test";

// Simple test that ensures 2-layer deep pages (counting from schema) are reachable.
// Smoketest was not sufficient (missing js files & returned path deviated from actual path)
test("test 2 level deep schema page being available", async ({ page }) => {
  await page.goto("/apps/ui/pet%20store/rdf/shacl");
  await Promise.all([
    expect(page.locator("html")).not.toContainText("File not found:"),
    expect(page.locator("h1")).toContainText("SHACL dashboard for pet store"),
  ]);
});
