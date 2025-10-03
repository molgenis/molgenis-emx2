import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe("Input Ontology", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}input/Ontology.story`);
  });

  test("when all child nodes are selected and one is deselected, then other sibling elements are shown in the filter well", async ({
    page,
  }) => {
    // expand
    await page
      .locator("#test-ontology-array-input-id-ontology")
      .getByRole("button", { name: "expand colors" })
      .click();

    // deselect "green"
    await page
      .locator("label")
      .filter({ hasText: "green" })
      .locator("rect")
      .click();

    // if "green" is deselected, then green should not be visible and other colors should be shown
    await expect(
      page
        .locator("#test-ontology-array-input-id-ontology button")
        .filter({ hasText: "green" })
    ).toHaveCount(0);
    await expect(page.getByRole("button", { name: "blue" })).toBeVisible();
    await expect(page.getByRole("button", { name: "purple" })).toBeVisible();
    await expect(page.getByRole("button", { name: "red" })).toBeVisible();
  });

  test("if all child nodes are selected, indeterminate and checked status are properly defined (false, true)", async ({
    page,
  }) => {
    await expect(
      page.locator("#test-ontology-array-input-id-colors-input + svg")
    ).toHaveAttribute("data-indeterminate", "false");

    await expect(
      page.locator("#test-ontology-array-input-id-colors-input + svg")
    ).toHaveAttribute("data-checked", "true");
  });

  test("if not all child nodes are selected, indeterminate and checked status are properly defined (true, false)", async ({
    page,
  }) => {
    await expect(
      page.locator("#test-ontology-array-input-id-species-input + svg")
    ).toHaveAttribute("data-indeterminate", "true");
    await expect(
      page.locator("#test-ontology-array-input-id-species-input + svg")
    ).toHaveAttribute("data-checked", "false");
  });

  test("if all child nodes are selected via the parent, then all child elements should be selected", async ({
    page,
  }) => {
    const speciesInput = page.locator(
      "#test-ontology-array-input-id-species-input + svg"
    );
    await speciesInput.click();

    await expect(speciesInput).toHaveAttribute("data-indeterminate", "false");
    await expect(speciesInput).toHaveAttribute("data-checked", "true");

    await page
      .locator("#test-ontology-array-input-id-ontology")
      .getByRole("button", { name: "expand species" })
      .click({ delay: 100 });

    await page
      .locator("#test-ontology-array-input-id-ontology")
      .getByRole("button", { name: "expand mammals" })
      .click({ delay: 100 });

    await expect(
      page.locator("#test-ontology-array-input-id-birds-input")
    ).toBeChecked();
    await expect(
      page.locator("#test-ontology-array-input-id-insect-input")
    ).toBeChecked();
    await expect(
      page.locator("#test-ontology-array-input-id-mammals-input")
    ).toBeChecked();
  });
});
