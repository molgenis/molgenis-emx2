import { test, expect } from "@playwright/test";
import playwrightConfig from "~/playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}input/Ontology.story`);
  await page
    .getByText("InputOntology", { exact: true })
    .first()
    .click({ delay: 300 });
});

test("test expand and deselect behavior", async ({ page }) => {
  //expand
  await page
    .locator("#test-ontology-array-input-id-checkbox-group")
    .getByRole("button", { name: "expand colors" })
    .click();
  //select
  await page
    .locator("label")
    .filter({ hasText: "green" })
    .locator("rect")
    .click();
  await expect(page.getByRole("button", { name: "blue" })).toBeVisible();
  await expect(
    page
      .locator("#test-ontology-array-input-id-green-input")
      .locator("xpath=following-sibling::*")
      .locator("svg")
  ).toHaveAttribute("data-indeterminate");
  await expect(
    page
      .locator("#test-ontology-array-input-id-colors-input")
      .locator("xpath=following-sibling::*")
      .locator("svg")
  ).toHaveAttribute("data-checked");
  await page
    .locator("#test-ontology-array-input-id-checkbox-group")
    .getByRole("button", { name: "expand species" })
    .click();
  await page
    .locator("label")
    .filter({ hasText: "mammals" })
    .locator("rect")
    .click();
  await expect(
    page
      .locator("#test-ontology-array-input-id-checkbox-group label")
      .filter({ hasText: "species" })
      .locator("rect")
  ).toBeVisible();
  //search
  await page.getByRole("button", { name: "Search" }).nth(1).click();
  await page.getByPlaceholder("Search in terms").click();
  await page.getByPlaceholder("Search in terms").fill("re");
  await expect(
    page
      .locator("#test-ontology-array-input-id-checkbox-group")
      .getByText("colors", { exact: true })
  ).toBeVisible();
  await expect(
    page
      .locator("#test-ontology-array-input-id-checkbox-group")
      .getByText("species", { exact: true })
  ).toBeVisible();
});
