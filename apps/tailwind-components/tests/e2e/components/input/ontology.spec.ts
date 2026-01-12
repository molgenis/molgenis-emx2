import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("Input Ontology", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}input/Ontology.story`);
  });

  test("small ontologies are shown expanded", async ({ page }) => {
    // Small ontologies should be visible (expanded by default)
    await expect(
      page
        .locator("#test-ontology-array-input-id-input-ontology")
        .filter({ hasText: "green" })
    ).toBeVisible();
    await expect(
      page
        .locator("#test-ontology-array-input-id-input-ontology")
        .filter({ hasText: "blue" })
    ).toBeVisible();
  });

  test("large ontologies are shown as select", async ({ page }) => {
    const ontologyContainer = page.locator(
      "#test-ontology-array-input-id2-input-ontology"
    );

    await expect(
      ontologyContainer.getByRole("button", { name: "Andorra" })
    ).toBeVisible();

    await ontologyContainer.locator("svg.text-input").last().click();

    await ontologyContainer.getByText("American Samoa").click();

    await expect(
      ontologyContainer.getByRole("button", { name: "American Samoa" })
    ).toBeVisible();
  });

  test("if all child nodes are selected, indeterminate and checked status are properly defined (false, true)", async ({
    page,
  }) => {
    await expect(
      page.locator("#test-ontology-array-input-id-input-colors-input + svg")
    ).toHaveAttribute("data-indeterminate", "false");

    await expect(
      page.locator("#test-ontology-array-input-id-input-species-input + svg")
    ).toHaveAttribute("data-indeterminate", "true");
  });

  test("if all child nodes are selected via the parent, then all child elements should be selected", async ({
    page,
  }) => {
    const speciesInput = page.locator(
      "#test-ontology-array-input-id-input-species-input + svg"
    );
    await speciesInput.click();

    await expect(speciesInput).toHaveAttribute("data-indeterminate", "false");
    await expect(speciesInput).toHaveAttribute("data-checked", "true");

    await expect(
      page.locator("#test-ontology-array-input-id-input-birds-input")
    ).toBeChecked();
    await expect(
      page.locator("#test-ontology-array-input-id-input-insect-input")
    ).toBeChecked();
    await expect(
      page.locator("#test-ontology-array-input-id-input-mammals-input")
    ).toBeChecked();
  });
});
