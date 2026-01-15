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
    await expect(
      page
        .locator("#test-ontology-array-input-id-ontology")
        .filter({ hasText: "green" })
    ).toHaveCount(0);
    await expect(
      page
        .locator("#test-ontology-array-input-id-ontology")
        .filter({ hasText: "blue" })
    ).toHaveCount(0);
  });

  test("large ontologies are shown as select", async ({ page }) => {
    await expect(
      page
        .locator("#test-ontology-input-id2-input-ontology")
        .getByRole("button", { name: "Andorra" })
    ).toBeVisible();
    await page
      .locator(
        "#test-ontology-input-id2-input-ontology > .flex.items-center.justify-between > div:nth-child(2) > svg:nth-child(2)"
      )
      .click();
    await page
      .locator("#test-ontology-input-id2-input-ontology")
      .getByText("American Samoa")
      .click();
    await expect(
      page.getByRole("button", { name: "American Samoa" })
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
