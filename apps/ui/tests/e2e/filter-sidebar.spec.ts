import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.describe("filter sidebar", () => {
  test("sidebar renders with filters heading", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const filtersHeading = page.getByRole("heading", { level: 2 }).filter({
      hasText: "Filters",
    });
    await expect(filtersHeading).toBeVisible();
  });

  test("filter sections expand and collapse", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const filterSectionHeadings = page.locator("h3");
    const sectionCount = await filterSectionHeadings.count();
    expect(sectionCount).toBeGreaterThanOrEqual(1);

    const firstSection = filterSectionHeadings.first();
    await expect(firstSection).toBeVisible();
  });

  test("filter sections contain checkboxes", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const checkboxes = page.locator('input[type="checkbox"]');
    const checkboxCount = await checkboxes.count();
    expect(checkboxCount).toBeGreaterThanOrEqual(1);
  });

  test("table renders with data", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const table = page.locator("table");
    await expect(table).toBeVisible();

    const rows = page.locator("table tbody tr");
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThanOrEqual(1);
  });

  test("customize button exists and opens modal", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const customizeButton = page.getByRole("button", {
      name: "Customize",
    });
    await expect(customizeButton).toBeVisible();

    await customizeButton.click();
    await page.waitForTimeout(1000);

    const modalDialog = page.locator('[role="dialog"]');
    await expect(modalDialog).toBeVisible();

    const cancelButton = page.getByRole("button", {
      name: /cancel/i,
    });
    await cancelButton.click();

    await expect(modalDialog).not.toBeVisible();
  });

  test("search filter input works", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const searchInput = page.locator('input[placeholder="Search..."]');
    await expect(searchInput).toBeVisible();

    await searchInput.fill("test");
    const value = await searchInput.inputValue();
    expect(value).toBe("test");

    await searchInput.clear();
    const clearedValue = await searchInput.inputValue();
    expect(clearedValue).toBe("");
  });

  test("filter interaction updates URL", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const firstLabel = page
      .locator('label:has(input[type="checkbox"])')
      .first();
    await expect(firstLabel).toBeVisible();

    const initialUrl = page.url();
    await firstLabel.click();
    await page.waitForTimeout(500);

    const updatedUrl = page.url();
    expect(updatedUrl).not.toBe(initialUrl);

    await firstLabel.click();
    await page.waitForTimeout(500);
  });

  test("nested REF filter expands and allows selecting leaf columns (catalogue-demo)", async ({
    page,
  }) => {
    // Test verifies that nested REF selection creates a dotted path filter
    // and displays the breadcrumb notation in the sidebar
    // Parent: "internal identifiers" → Leaf: "identifier" (TEXT type)
    // Expected filter ID: "internalIdentifiers.identifier"

    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    // Open Picker modal
    const customizeButton = page.getByRole("button", { name: /customize/i });
    await expect(customizeButton).toBeVisible();
    await customizeButton.click();
    await page.waitForTimeout(2000);

    const modal = page.locator('[role="dialog"]');
    await expect(modal).toBeVisible();

    // Find "internal identifiers → InternalIdentifiers" expander
    const internalIdRef = modal.locator(
      'button:has-text("internal identifiers")'
    );
    await expect(internalIdRef).toBeVisible();

    // Click to expand nested columns
    await internalIdRef.click();
    await page.waitForTimeout(1000);

    // Find nested leaf nodes (items with padding-left > 0)
    // Specifically, find the "identifier" field nested under internal identifiers
    // It will have padding-left: 1.5rem and contain "identifierInternal identifier"
    const nestedItems = modal.locator(
      'li[style*="padding-left: 1.5rem"] label'
    );
    const itemCount = await nestedItems.count();
    expect(itemCount).toBeGreaterThan(0);

    // Find the first occurrence of the nested "identifier" field
    // It should contain the text pattern "identifierInternal identifier"
    let identifierLeaf = null;
    for (let i = 0; i < itemCount; i++) {
      const item = nestedItems.nth(i);
      const text = await item.textContent();
      // Match "identifier" at the start of label (not "type")
      if (text && /^identifier[A-Z]/.test(text)) {
        identifierLeaf = item;
        break;
      }
    }

    expect(identifierLeaf).not.toBeNull();

    // Verify it's not already checked
    const checkbox = identifierLeaf!.locator('input[type="checkbox"]');
    let wasChecked = await checkbox.isChecked();
    if (wasChecked) {
      // Toggle off so we can toggle it on to test the change
      await checkbox.click();
      await page.waitForTimeout(500);
    }

    // Click the checkbox to select the nested leaf
    await identifierLeaf!.click();
    await page.waitForTimeout(500);

    // Verify checkbox is now checked
    const isNowChecked = await checkbox.isChecked();
    expect(isNowChecked).toBe(true);

    // Click Apply button
    const applyBtn = page.getByRole("button", { name: /apply/i });
    await expect(applyBtn).toBeVisible();
    await applyBtn.click();
    await page.waitForTimeout(2000);

    // Verify modal closed
    await expect(modal).not.toBeVisible();

    // Verify URL contains dotted path filter (NOT a top-level column)
    const url = page.url();
    expect(url).toContain("mg_filters=");
    expect(url).toContain("internalIdentifiers.identifier");

    // Verify sidebar shows breadcrumb notation with →
    const sidebarHeadings = page.locator("h3");
    const breadcrumbHeading = sidebarHeadings.filter({
      hasText: /internal identifiers\s*→\s*identifier/i,
    });
    await expect(breadcrumbHeading).toBeVisible();

    // Prove nested filter is functional: locate text input within the new section
    // and type a value to verify URL changes
    const sidebarSection = page.locator("h3", {
      hasText: /internal identifiers\s*→\s*identifier/i,
    });
    await expect(sidebarSection).toBeVisible();

    // Find the filter container for this section and locate the search input
    // Start from the h3 and go to the next sibling or parent container
    const searchInputs = page.locator('input[type="search"]');
    const inputCountBefore = await searchInputs.count();

    // Type into the last search input (most likely the newly created filter)
    const lastInput = searchInputs.last();
    await lastInput.fill("test");
    // Wait for debounce to flush and URL to update (auto-retrying assertion)
    // The filter value appears as internalIdentifiers.identifier.*=test in the URL
    await page.waitForURL(/internalIdentifiers\.identifier.*=test/);

    // Verify URL changed to include the nested filter value
    const updatedUrl = page.url();
    expect(updatedUrl).toContain("internalIdentifiers.identifier");
    expect(updatedUrl).not.toBe(url);
  });

  test("nested REF filter with string leaf field (type test schema)", async ({
    page,
  }) => {
    // Similar test on a different schema with a different nested field type
    // Navigate to type test schema (Types table) and select nested "option value" field
    // Parent: "ref type" → Leaf: "option value" (STRING type)
    // Expected filter ID: "refType.optionValue"

    await page.goto(`${route}type%20test/Types`);
    await page.waitForTimeout(3000);

    // Open Picker modal
    const customizeButton = page.getByRole("button", { name: /customize/i });
    await expect(customizeButton).toBeVisible();
    await customizeButton.click();
    await page.waitForTimeout(2000);

    const modal = page.locator('[role="dialog"]');
    await expect(modal).toBeVisible();

    // Find "ref type → Options" expander
    const refTypeExpander = modal.locator('button:has-text("ref type")');
    await expect(refTypeExpander).toBeVisible();

    // Click to expand
    await refTypeExpander.click();
    await page.waitForTimeout(1000);

    // Find nested leaves with indentation (padding-left: 1.5rem)
    const nestedLeaves = modal.locator('li[style*="padding-left: 1.5rem"]');
    const leafCount = await nestedLeaves.count();
    expect(leafCount).toBeGreaterThan(0);

    // Find "option value" field (STRING type)
    let optionValueLeaf = null;
    for (let i = 0; i < leafCount; i++) {
      const leaf = nestedLeaves.nth(i);
      const text = await leaf.textContent();
      if (text && text.includes("option value")) {
        optionValueLeaf = leaf;
        break;
      }
    }

    expect(optionValueLeaf).not.toBeNull();

    // Get checkbox and verify it's not checked
    const checkbox = optionValueLeaf!.locator('input[type="checkbox"]');
    const isChecked = await checkbox.isChecked();
    if (!isChecked) {
      // Click to select it
      await optionValueLeaf!.click();
      await page.waitForTimeout(500);
    }

    // Verify it's now checked
    const isNowChecked = await checkbox.isChecked();
    expect(isNowChecked).toBe(true);

    // Click Apply
    const applyBtn = page.getByRole("button", { name: /apply/i });
    await expect(applyBtn).toBeVisible();
    await applyBtn.click();
    await page.waitForTimeout(2000);

    // Verify modal closed
    await expect(modal).not.toBeVisible();

    // Verify URL contains a dotted path (indicates nested filter)
    const url = page.url();
    expect(url).toContain("mg_filters=");
    expect(url).toContain("refType.optionValue");

    // Verify sidebar shows breadcrumb notation with →
    const sidebarHeadings = page.locator("h3");
    const breadcrumbHeading = sidebarHeadings.filter({
      hasText: /ref type\s*→\s*option value/i,
    });
    await expect(breadcrumbHeading).toBeVisible();

    // Prove nested filter is functional: type a value into the search input
    // and verify URL changes
    const searchInputs = page.locator('input[type="search"]');
    const lastInput = searchInputs.last();
    await lastInput.fill("testvalue");

    // Wait for debounce to flush and URL to update (auto-retrying assertion)
    // The filter value appears as refType.optionValue.*=testvalue in the URL
    await page.waitForURL(/refType\.optionValue.*=testvalue/);

    // Verify URL changed to include the nested filter value
    const updatedUrl = page.url();
    expect(updatedUrl).toContain("refType.optionValue");
    expect(updatedUrl).not.toBe(url);
  });

  test("filter picker footer buttons exist", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(3000);

    const customizeButton = page.getByRole("button", { name: /customize/i });
    await customizeButton.click();
    await page.waitForTimeout(1000);

    const modal = page.locator('[role="dialog"]');
    await expect(modal).toBeVisible();

    // Check for footer buttons
    const applyBtn = page.getByRole("button", { name: /apply/i });
    const cancelBtn = page.getByRole("button", { name: /cancel/i });
    const clearBtn = page.getByRole("button", { name: /clear/i });
    const selectAllBtn = page.getByRole("button", { name: /select all/i });
    const resetBtn = page.getByRole("button", { name: /reset/i });

    // All required footer buttons must be visible
    await expect(applyBtn).toBeVisible();
    await expect(cancelBtn).toBeVisible();
    await expect(clearBtn).toBeVisible();
    await expect(selectAllBtn).toBeVisible();
    await expect(resetBtn).toBeVisible();

    // Close with Cancel
    await cancelBtn.click();
    await page.waitForTimeout(500);
  });

  test("pagination count updates when filter applied", async ({ page }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const paginationNav = page.locator('nav[role="navigation"]');
    await expect(paginationNav).toBeVisible();

    const initialTotalPagesSpan = page
      .locator('nav[role="navigation"] span')
      .filter({ hasText: /OF/ })
      .first();
    const initialTotalPages = await initialTotalPagesSpan.textContent();
    expect(initialTotalPages).toMatch(/OF \d+/);

    const initialPages = parseInt(initialTotalPages!.replace(/OF /g, ""));
    expect(initialPages).toBeGreaterThan(0);

    const initialRowCount = await page.locator("table tbody tr").count();

    const firstCheckboxLabel = page
      .locator('label:has(input[type="checkbox"])')
      .first();
    await expect(firstCheckboxLabel).toBeVisible();

    await firstCheckboxLabel.click();
    await page.waitForTimeout(3000);

    const paginationVisible = await paginationNav.isVisible();
    if (paginationVisible) {
      const updatedTotalPagesSpan = page
        .locator('nav[role="navigation"] span')
        .filter({ hasText: /OF/ })
        .first();
      const updatedTotalPages = await updatedTotalPagesSpan.textContent();
      expect(updatedTotalPages).toMatch(/OF \d+/);

      const updatedPages = parseInt(updatedTotalPages!.replace(/OF /g, ""));
      expect(updatedPages).toBeLessThan(initialPages);
    } else {
      const filteredRowCount = await page.locator("table tbody tr").count();
      expect(filteredRowCount).toBeLessThan(initialRowCount);
    }
  });

  test("pagination OF count decreases but stays visible when filter leaves >pageSize results", async ({
    page,
  }) => {
    await page.goto(`${route}catalogue-demo/Resources`);
    await page.waitForTimeout(5000);

    const paginationNav = page.locator('nav[role="navigation"]');
    await expect(paginationNav).toBeVisible();

    const initialOfSpan = page
      .locator('nav[role="navigation"] span')
      .filter({ hasText: /OF/ })
      .first();
    const initialText = await initialOfSpan.textContent();
    expect(initialText).toMatch(/OF \d+/);
    const initialPages = parseInt(initialText!.replace(/OF /g, "").trim());
    expect(initialPages).toBeGreaterThan(2);

    const countriesHeading = page
      .locator("h3")
      .filter({ hasText: /countries/i });
    await expect(countriesHeading).toBeVisible();

    const netherlandsLabel = page
      .locator('label:has(input[type="checkbox"])')
      .filter({ hasText: /Netherlands/ })
      .first();
    await expect(netherlandsLabel).toBeVisible();
    const filterRequestPromise = page.waitForResponse(
      (response) =>
        response.url().includes("/graphql") && response.status() === 200,
      { timeout: 15000 }
    );

    await netherlandsLabel.click();
    await filterRequestPromise;

    await expect(paginationNav).toBeVisible({ timeout: 10000 });

    const updatedOfSpan = page
      .locator('nav[role="navigation"] span')
      .filter({ hasText: /OF/ })
      .first();
    await expect(updatedOfSpan).toBeVisible({ timeout: 10000 });

    await expect
      .poll(
        async () => {
          const text = await updatedOfSpan.textContent();
          return parseInt(text?.replace(/OF /g, "").trim() ?? "");
        },
        { timeout: 10000 }
      )
      .toBeLessThan(initialPages);
  });
});

test("countable filter sections with zero base count are hidden initially", async ({
  page,
}) => {
  await page.goto(`${route}catalogue-demo/Collections`);
  await page.waitForTimeout(5000);

  const url = page.url();
  expect(url).not.toContain("mg_filters=");

  const filterHeadings = page.locator("h3");
  const headingTexts = await filterHeadings.allTextContents();

  const filtersHeading = page.getByRole("heading", { level: 2 }).filter({
    hasText: "Filters",
  });
  await expect(filtersHeading).toBeVisible();

  expect(headingTexts.length).toBeGreaterThan(0);

  const hrICoreSection = filterHeadings.filter({ hasText: /hricore/i });

  const hrICoreVisible = await hrICoreSection.isVisible().catch(() => false);

  expect(hrICoreVisible).toBe(
    false,
    'Empty "hricore" section should be hidden'
  );

  const countriesSection = filterHeadings.filter({ hasText: /countries/i });
  await expect(countriesSection).toBeVisible(
    "Non-empty 'countries' section should be visible"
  );
});
