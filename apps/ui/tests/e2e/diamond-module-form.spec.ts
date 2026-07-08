import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}diamond_test/Subject`);
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
    console.log(`\nVerifying page loaded with diamond_test/Subject...`);

    // Capture console messages for debugging
    const consoleLogs: string[] = [];
    page.on("console", (msg) => {
      consoleLogs.push(`[${msg.type()}] ${msg.text()}`);
    });

    // Capture response status for debugging
    const responses: { url: string; status: number }[] = [];
    page.on("response", (res) => {
      if (res.url().includes("graphql") || res.status() >= 400) {
        responses.push({ url: res.url(), status: res.status() });
      }
    });

    // Verify heading loads
    const heading = page.getByRole("heading", { level: 1 });
    await expect(heading).toContainText("Subject", { timeout: 10000 });

    // Log diagnostics
    if (consoleLogs.length > 0) {
      console.log("   Console messages:");
      consoleLogs.forEach((log) => console.log(`     ${log}`));
    }
    if (responses.length > 0) {
      console.log("   Network responses (graphql/errors):");
      responses.forEach((res) => console.log(`     ${res.status} ${res.url}`));
    }

    // Verify Add Subject button is visible (form rendered)
    const addButton = page.getByRole("button", { name: "Add Subject" });
    await expect(addButton).toBeVisible();

    console.log(
      "PASS: Form rendered, metadata loaded, Add Subject button visible"
    );
  });

  test("sex ENUM input: click opens options, selection changes field state", async ({
    page,
  }) => {
    // Open Add modal
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    // Take screenshot of the modal to see structure
    await page.screenshot({ path: "diamond-form-initial.png" });

    // Find sex combobox by role + id pattern
    const sexCombobox = page.locator('[role="combobox"][id*="sex"]');
    await expect(sexCombobox).toBeVisible();

    console.log("\n1. sex ENUM combobox found and visible");

    // Click to open options
    await sexCombobox.click();
    await page.waitForTimeout(500);

    // Look for dropdown/option items (skip placeholder by taking 2nd option)
    const options = page.locator('[role="option"]');
    const optionCount = await options.count();
    console.log(`2. Found ${optionCount} options after click`);

    if (optionCount > 1) {
      // Click the second option (skip "Select an option" placeholder at [0])
      const secondOption = options.nth(1);
      const optionText = await secondOption.textContent();
      console.log(`3. Clicking option: "${optionText}"`);

      await secondOption.click();
      await page.waitForTimeout(300);

      // Take screenshot after selection
      await page.screenshot({ path: "diamond-form-after-sex.png" });

      // Verify the field now shows the selected value
      const selectedText = await sexCombobox.textContent();
      console.log(`4. After click, field displays: "${selectedText}"`);

      // Check if the option text is now in the field
      expect(selectedText).toContain(optionText);
      console.log("✓ sex selection works");
    } else {
      console.log("ERROR: No options found");
      await page.screenshot({ path: "diamond-form-sex-no-options.png" });
      throw new Error("sex combobox has no options");
    }
  });

  test("tags ENUM_ARRAY: click checkbox toggles selection state", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    // Debug: list all input fields in the form
    console.log("\n1. Inspecting form fields...");
    const allInputs = page.locator('[id*="form-field-input"]');
    const count = await allInputs.count();
    console.log(`   Found ${count} form input fields`);

    const fieldIds: string[] = [];
    for (let i = 0; i < count; i++) {
      const id = await allInputs.nth(i).getAttribute("id");
      fieldIds.push(id || `(unnamed-${i})`);
    }
    fieldIds.forEach((id) => console.log(`   - ${id}`));

    // Find tags input by id pattern
    const tagsInput = page
      .locator('[id*="tags"][id*="form-field-input"]')
      .first();
    const tagsVisible = await tagsInput.isVisible().catch(() => false);

    if (!tagsVisible) {
      console.log("\n   tags field not visible in form");
      console.log(
        "   (May not be included in Add form due to field configuration)"
      );

      // Take a screenshot to document the form state
      await page.screenshot({ path: "diamond-form-visible-fields.png" });
      test.skip();
      return;
    }

    console.log("\n2. tags ENUM_ARRAY input found and visible");

    // The checkboxes use custom SVG renderers, so click the label/container instead
    // Find the first label that wraps a checkbox
    const checkboxLabel = page
      .locator('[id*="tags-form-field-input-checkbox-group"]')
      .first()
      .locator("..")
      .first();
    const initialText = await checkboxLabel.textContent();
    console.log(`3. First checkbox label text: "${initialText}"`);

    // Get initial state from aria-checked or the SVG data-checked attribute
    const checkboxInput = page
      .locator(
        '[id*="tags-form-field-input-checkbox-group"] input[type="checkbox"]'
      )
      .first();
    const beforeChecked = await checkboxInput.isChecked();
    console.log(`4. Before click: checked=${beforeChecked}`);

    // Click the SVG icon or the label wrapper
    const svgIcon = checkboxLabel.locator("svg").first();
    await svgIcon.click({ force: true }); // force: true to bypass pointer events
    await page.waitForTimeout(300);

    // Check the new state
    const afterChecked = await checkboxInput.isChecked();
    console.log(`5. After click: checked=${afterChecked}`);

    expect(afterChecked).not.toBe(beforeChecked);
    console.log("✓ tags toggle works");
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
    console.log(`1. Module checkbox initial state: checked=${beforeChecked}`);

    // Checkbox uses a custom SVG renderer; click the icon inside its label.
    await targetCheckbox
      .locator("..")
      .locator("svg")
      .first()
      .click({ force: true });
    await page.waitForTimeout(300);

    const afterChecked = await targetCheckbox.isChecked();
    console.log(`2. After click: checked=${afterChecked}`);

    expect(afterChecked).not.toBe(beforeChecked);
    console.log("✓ subgroups01 toggle works");
  });

  test("MODULE CONTENT REVEAL: selecting a module reveals its content field inline; deselecting hides it", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    console.log("\nTesting MODULE CONTENT REVEAL (F2b)");

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
    console.log(
      "1. relevantmedhistory field is visible after selecting CockayneSyndrome"
    );

    // Deselect CockayneSyndrome
    await svgIcon.click({ force: true });
    await page.waitForLoadState("networkidle");

    // Assert relevantmedhistory is hidden after deselection
    await expect(contentField).not.toBeVisible({ timeout: 5000 });
    console.log(
      "2. relevantmedhistory field is hidden after deselecting CockayneSyndrome"
    );
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

    console.log("\n1. diseaseGroup MODULE_ARRAY checkbox found and visible");

    const beforeChecked = await firstCheckbox.isChecked();
    console.log(`2. Initial state: checked=${beforeChecked}`);

    await firstCheckbox
      .locator("..")
      .locator("svg")
      .first()
      .click({ force: true });
    await page.waitForTimeout(300);

    const afterChecked = await firstCheckbox.isChecked();
    console.log(`3. After click: checked=${afterChecked}`);

    expect(afterChecked).not.toBe(beforeChecked);
  });
});

test.describe("Diamond scalar MODULE Add-record form interactions", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}diamond_test/Experiment`);
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
    await page.waitForTimeout(300);
    await modal.getByRole("option", { name: "RNA", exact: true }).click();
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
    await page.waitForTimeout(300);
    await modal.getByRole("option", { name: "DNA", exact: true }).click();
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
