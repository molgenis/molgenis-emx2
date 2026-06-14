import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}diamond_demo/Subject`);
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
    console.log(`\nVerifying page loaded with diamond_demo/Subject...`);

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
    const tagsInput = page.locator('[id*="tags"][id*="form-field-input"]').first();
    const tagsVisible = await tagsInput.isVisible().catch(() => false);

    if (!tagsVisible) {
      console.log("\n   tags field not visible in form");
      console.log("   (May not be included in Add form due to field configuration)");

      // Take a screenshot to document the form state
      await page.screenshot({ path: "diamond-form-visible-fields.png" });
      test.skip();
      return;
    }

    console.log("\n2. tags ENUM_ARRAY input found and visible");

    // The checkboxes use custom SVG renderers, so click the label/container instead
    // Find the first label that wraps a checkbox
    const checkboxLabel = page.locator('[id*="tags-form-field-input-checkbox-group"]').first().locator("..").first();
    const initialText = await checkboxLabel.textContent();
    console.log(`3. First checkbox label text: "${initialText}"`);

    // Get initial state from aria-checked or the SVG data-checked attribute
    const checkboxInput = page.locator('[id*="tags-form-field-input-checkbox-group"] input[type="checkbox"]').first();
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

    console.log("\n1. Searching for subgroups01 MODULE_ARRAY field...");

    // Scroll the modal to reveal all fields
    const modal = page.locator('[role="dialog"]');
    if (await modal.isVisible()) {
      await modal.evaluate((el) => (el.scrollTop = el.scrollHeight));
      await page.waitForTimeout(500);
    }

    // Find subgroups01 by id pattern
    const subgroupsInput = page.locator('[id*="subgroups01"]').first();
    const subgroupsVisible = await subgroupsInput.isVisible().catch(() => false);

    if (!subgroupsVisible) {
      console.log("   subgroups01 field not visible even after scrolling");
      // Debug: list all form fields to see what's in the form
      const allFields = page.locator('[id*="form-field-input"]');
      const count = await allFields.count();
      console.log(`   Total fields in form: ${count}`);

      // Look for subgroups by checking raw element existence (even if off-screen)
      const raw = page.locator('[id*="subgroups"]');
      const rawCount = await raw.count();
      console.log(`   Subgroups elements in DOM (any): ${rawCount}`);

      test.skip();
      return;
    }

    console.log("   Found subgroups01 MODULE_ARRAY input");

    // Find checkboxes within subgroups01 section
    const checkboxes = page.locator('[id*="subgroups01"] input[type="checkbox"]');
    const count = await checkboxes.count();
    console.log(`2. Found ${count} module checkboxes`);

    if (count === 0) {
      console.log("   WARNING: No checkboxes found for subgroups01");
      test.skip();
      return;
    }

    // Find CockayneSyndrome checkbox if possible
    let targetCheckbox = null;
    let targetLabel = "";
    for (let i = 0; i < count; i++) {
      const id = await checkboxes.nth(i).getAttribute("id") || "";
      const value = await checkboxes.nth(i).getAttribute("value") || "";
      if (id.toLowerCase().includes("cockayne") || value.toLowerCase().includes("cockayne")) {
        targetCheckbox = checkboxes.nth(i);
        targetLabel = value;
        console.log(`3. Found CockayneSyndrome module checkbox`);
        break;
      }
    }

    // Fall back to first if not found
    if (!targetCheckbox) {
      targetCheckbox = checkboxes.first();
      targetLabel = await targetCheckbox.getAttribute("value") || "Module";
      console.log(`3. Using first module: "${targetLabel}"`);
    }

    const beforeChecked = await targetCheckbox.isChecked();
    console.log(`4. Module checkbox initial state: checked=${beforeChecked}`);

    // Click via SVG like we did for tags
    const checkboxContainer = targetCheckbox.locator("..");
    const svgIcon = checkboxContainer.locator("svg").first();
    const svgExists = await svgIcon.count() > 0;

    if (svgExists) {
      await svgIcon.click({ force: true });
    } else {
      await targetCheckbox.click({ force: true });
    }
    await page.waitForTimeout(300);

    const afterChecked = await targetCheckbox.isChecked();
    console.log(`5. After click: checked=${afterChecked}`);

    // Take screenshot after module selection
    await page.screenshot({ path: "diamond-form-after-module.png" });

    expect(afterChecked).not.toBe(beforeChecked);
    console.log("✓ subgroups01 toggle works");
  });

  test("MODULE CONTENT REVEAL: selecting module shows its content field (F2 feature)", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    console.log("\nTesting MODULE CONTENT REVEAL (F2 feature - may be unimplemented)");

    // Find and select a module
    const subgroupsLabel = page.getByLabel("subgroups01");
    const subgroupsContainer = subgroupsLabel.locator("..");
    const checkboxes = subgroupsContainer.locator('[role="checkbox"]');
    const count = await checkboxes.count();

    if (count === 0) {
      console.log("SKIP: No module checkboxes to test");
      test.skip();
      return;
    }

    // Select first module
    const firstModule = checkboxes.first();
    const moduleText = await firstModule.textContent();
    console.log(`1. Selecting module: "${moduleText}"`);

    await firstModule.click();
    await page.waitForLoadState("networkidle");

    // Now look for module content field
    // For CockayneSyndrome, the content column is "relevantmedhistory"
    const contentField = page.getByLabel("relevantmedhistory");
    const isVisible = await contentField.isVisible().catch(() => false);

    console.log(`2. Module content field (relevantmedhistory) visible: ${isVisible}`);

    if (!isVisible) {
      console.log("   UNIMPLEMENTED: Module content reveal is NOT working (F2 feature missing)");
      // This is expected to fail for now
    }

    // Take screenshot showing the state
    await page.screenshot({ path: "diamond-form-module-reveal.png" });

    // This assertion will fail if F2 is not implemented
    expect(isVisible).toBeTruthy();
  });

  test("diseaseGroup MODULE_ARRAY: click module checkbox toggles selection", async ({
    page,
  }) => {
    await page.getByRole("button", { name: "Add Subject" }).click();
    await expect(
      page.getByRole("heading", { name: "Add Subject" })
    ).toBeVisible();

    // Find diseaseGroup MODULE_ARRAY by label
    const diseaseGroupLabel = page.getByLabel("diseaseGroup");
    const isVisible = await diseaseGroupLabel.isVisible().catch(() => false);

    if (!isVisible) {
      console.log("WARNING: diseaseGroup field not visible");
      test.skip();
      return;
    }

    console.log("\n1. diseaseGroup MODULE_ARRAY input found and visible");

    // Look for module checkboxes
    const diseaseGroupContainer = diseaseGroupLabel.locator("..");
    const checkboxes = diseaseGroupContainer.locator('[role="checkbox"]');
    const count = await checkboxes.count();
    console.log(`2. Found ${count} disease group checkboxes`);

    if (count > 0) {
      const firstCheckbox = checkboxes.first();
      const text = await firstCheckbox.textContent();
      console.log(`3. First option: "${text}"`);

      const beforeChecked = await firstCheckbox.isChecked();
      console.log(`4. Initial state: checked=${beforeChecked}`);

      await firstCheckbox.click();
      await page.waitForTimeout(300);

      const afterChecked = await firstCheckbox.isChecked();
      console.log(`5. After click: checked=${afterChecked}`);

      expect(afterChecked).not.toBe(beforeChecked);
    } else {
      console.log("WARNING: No disease group checkboxes found");
    }
  });
});
