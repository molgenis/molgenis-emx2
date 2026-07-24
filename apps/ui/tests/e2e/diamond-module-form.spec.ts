import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}diamond_showcase/Subject`);
  // Wait for page to fully load
  await page.waitForLoadState("networkidle");
});

test.describe("Diamond MODULE_ARRAY Add-record form interactions", () => {
  /**
   * Precondition gate: verify the form can actually render on the test environment.
   * Tests that follow depend on this setup working correctly.
   */
  test("PRECONDITION: Form renders and Add Subject control is visible", async ({
    page,
  }) => {
    // Verify heading loads
    const heading = page.getByRole("heading", { level: 1 });
    await expect(heading).toContainText("Subject", { timeout: 10000 });

    // Verify Add Subject button is visible (form rendered)
    const addButton = page.getByRole("button", { name: "Add Subject" });
    await expect(addButton).toBeVisible();
  });

  test("sex ENUM input: click opens options, selection changes field state", async ({
    page,
  }) => {
    // Open Add modal
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    // Find sex combobox by role + id pattern
    const sexCombobox = page.locator('[role="combobox"][id*="sex"]');
    await expect(sexCombobox).toBeVisible();

    // Click to open options
    await sexCombobox.click();

    // Look for dropdown/option items (skip placeholder by taking 2nd option)
    const options = page.locator('[role="option"]');
    await expect(options.nth(1)).toBeVisible();

    // Click the second option (skip "Select an option" placeholder at [0])
    const secondOption = options.nth(1);
    const optionText = await secondOption.textContent();
    expect(optionText).toBeTruthy();

    await secondOption.click();

    // Verify the field now shows the selected value
    await expect(sexCombobox).toContainText(optionText!);
  });

  test("tags ENUM_ARRAY: click checkbox toggles selection state", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    // Find tags input by id pattern
    const tagsInput = page
      .locator('[id*="tags"][id*="form-field-input"]')
      .first();
    const tagsVisible = await tagsInput.isVisible().catch(() => false);

    if (!tagsVisible) {
      // tags may not be included in the Add form due to field configuration
      test.skip();
      return;
    }

    // The checkboxes use custom SVG renderers, so click the label/container instead
    // Find the first label that wraps a checkbox
    const checkboxLabel = page
      .locator('[id*="tags-form-field-input-checkbox-group"]')
      .first()
      .locator("..")
      .first();

    // Get initial state from aria-checked or the SVG data-checked attribute
    const checkboxInput = page
      .locator(
        '[id*="tags-form-field-input-checkbox-group"] input[type="checkbox"]'
      )
      .first();
    const beforeChecked = await checkboxInput.isChecked();

    // Click the SVG icon or the label wrapper
    const svgIcon = checkboxLabel.locator("svg").first();
    await svgIcon.click({ force: true }); // force: true to bypass pointer events

    await expect.poll(() => checkboxInput.isChecked()).not.toBe(beforeChecked);
  });

  test("subgroups01 MODULE_ARRAY: click module checkbox toggles selection", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    // Locate by accessible role/name, scoped to the modal, so the query
    // can't collide with the same-named sort button in the background table.
    const modal = page.getByRole("dialog");
    const targetCheckbox = modal.getByRole("checkbox", {
      name: "CockayneSyndrome",
    });
    await expect(targetCheckbox).toBeVisible();

    const beforeChecked = await targetCheckbox.isChecked();

    // Checkbox uses a custom SVG renderer; click the icon inside its label.
    await targetCheckbox
      .locator("..")
      .locator("svg")
      .first()
      .click({ force: true });

    await expect.poll(() => targetCheckbox.isChecked()).not.toBe(beforeChecked);
  });

  test("MODULE CONTENT REVEAL: selecting a module reveals its content field inline; deselecting hides it", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    const modal = page.getByRole("dialog");
    const cockaynCheckbox = modal.getByRole("checkbox", {
      name: "CockayneSyndrome",
    });
    await expect(cockaynCheckbox).toBeVisible();

    const svgIcon = cockaynCheckbox.locator("..").locator("svg").first();

    // Select CockayneSyndrome
    await svgIcon.click({ force: true });
    await page.waitForLoadState("networkidle");

    // Assert relevantmedhistory is visible after selection
    const contentField = page.getByLabel("relevantmedhistory");
    await expect(contentField).toBeVisible({ timeout: 5000 });

    // Deselect CockayneSyndrome
    await svgIcon.click({ force: true });
    await page.waitForLoadState("networkidle");

    // Assert relevantmedhistory is hidden after deselection
    await expect(contentField).not.toBeVisible({ timeout: 5000 });
  });

  test("diseaseGroup MODULE_ARRAY: click module checkbox toggles selection", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    // "diseaseGroup" is the checkbox-group section heading, not a <label for>
    // target, so getByLabel never matches it; query the checkbox by role/name
    // scoped to the modal instead.
    const modal = page.getByRole("dialog");
    const firstCheckbox = modal.getByRole("checkbox", {
      name: "EpidermolysisBullosa",
    });
    await expect(firstCheckbox).toBeVisible();

    const beforeChecked = await firstCheckbox.isChecked();

    await firstCheckbox
      .locator("..")
      .locator("svg")
      .first()
      .click({ force: true });

    await expect.poll(() => firstCheckbox.isChecked()).not.toBe(beforeChecked);
  });
});

test.describe("Diamond scalar MODULE Add-record form interactions", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}diamond_showcase/Experiment`);
    await page.waitForLoadState("networkidle");
  });

  test("experimentType MODULE: selecting RNA reveals RNA fields; selecting DNA swaps to DNA fields", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Experiment" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Experiment" })
    ).toBeVisible();

    const modal = page.getByRole("dialog");
    const experimentTypeCombobox = modal
      .locator('[role="combobox"][id*="experimentType"]')
      .first();
    await expect(experimentTypeCombobox).toBeVisible();

    // Select RNA
    await experimentTypeCombobox.click();
    const rnaOption = modal.getByRole("option", { name: "RNA", exact: true });
    await expect(rnaOption).toBeVisible();
    await rnaOption.click();
    await page.waitForLoadState("networkidle");

    // Combobox reflects the selection (regression guard for the
    // defineModel()-stale-getter bug in Listbox.vue: a redundant manual
    // emit() clobbered the correct value with the pre-selection state).
    await expect(experimentTypeCombobox).toHaveText("RNA");

    // RNA module fields revealed
    await expect(modal.getByLabel("rnaConcentration")).toBeVisible({
      timeout: 5000,
    });
    await expect(modal.getByLabel("libraryPrep")).toBeVisible();
    await expect(modal.getByLabel("readCount")).toBeVisible();

    // DNA module fields not shown
    await expect(modal.getByLabel("dnaConcentration")).not.toBeVisible();

    // Switch to DNA
    await experimentTypeCombobox.click();
    const dnaOption = modal.getByRole("option", { name: "DNA", exact: true });
    await expect(dnaOption).toBeVisible();
    await dnaOption.click();
    await page.waitForLoadState("networkidle");

    await expect(experimentTypeCombobox).toHaveText("DNA");

    // DNA module fields revealed
    await expect(modal.getByLabel("dnaConcentration")).toBeVisible({
      timeout: 5000,
    });
    await expect(modal.getByLabel("sequencingPlatform")).toBeVisible();
    await expect(modal.getByLabel("coverage")).toBeVisible();

    // RNA module fields no longer shown
    await expect(modal.getByLabel("rnaConcentration")).not.toBeVisible();
  });

  test("experimentId PK field is fillable exactly once and required-PK validation does not block save", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Experiment" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Experiment" })
    ).toBeVisible();

    const modal = page.getByRole("dialog");
    const experimentIdInput = modal.getByRole("textbox", {
      name: "experimentId Required",
    });

    // Regression guard: experimentId must appear exactly once in the DOM.
    // TableMetadata.getColumnsIncludingModules() previously duplicated the
    // shared root PK once per module subtype table (RNA, DNA), producing
    // colliding v-model bindings and a required-PK validation that could
    // never be satisfied.
    await expect(experimentIdInput).toHaveCount(1);
    await expect(experimentIdInput).toBeVisible();

    const uniqueId = `exp-pk-check-${Date.now()}`;
    await experimentIdInput.fill(uniqueId);
    await expect(experimentIdInput).toHaveValue(uniqueId);

    await page.getByRole("button", { name: "Save", exact: true }).click();
    await page.waitForLoadState("networkidle");

    await expect(
      page.getByRole("heading", { name: "Add Experiment" })
    ).not.toBeVisible();
  });
});
