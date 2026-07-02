import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test.describe("filter count parity regression (type test schema)", () => {
  test("filter count parity for facet filters with hard assertions", async ({
    page,
  }) => {
    const baseURL = playwrightConfig?.use?.baseURL || "http://localhost:3000/";
    const probeUrl = `${baseURL}type%20test/graphql`;

    try {
      const probeResponse = await page.request.post(probeUrl, {
        data: { query: "{ __typename }" },
      });
      if (probeResponse.status() !== 200) {
        test.skip(
          true,
          "type test schema not loaded (set MOLGENIS_INCLUDE_TYPE_TEST_DEMO=true)"
        );
      }
    } catch {
      test.skip(
        true,
        "type test schema not loaded (set MOLGENIS_INCLUDE_TYPE_TEST_DEMO=true)"
      );
    }

    const mg_filters = [
      "selectType",
      "selectTypeLabel",
      "radioType",
      "radioTypeLabel",
      "ontologyLargeType",
      "ontologySmallArrayType",
      "ontologyLargeArrayType",
      "ontologySmallTreeType",
      "ontologyLargeTreeType",
      "ontologySmallTreeArrayType",
      "ontologyLargeTreeArrayType",
      "showVisibleExpressionVariable",
      "additionalInfo",
      "stringType",
      "stringArrayType",
      "textType",
      "textArrayType",
      "autoIdType",
      "jsonType",
      "emailType",
      "emailArrayType",
      "hyperlinkType",
      "hyperlinkArrayType",
      "intType",
      "intArrayType",
    ].join(",");

    const url = `${route}type%20test/Types?mg_filters=${encodeURIComponent(
      mg_filters
    )}`;
    await page.goto(url);
    await page.waitForLoadState("networkidle", { timeout: 15000 });

    const filtersHeading = page.getByRole("heading", { level: 2 }).filter({
      hasText: "Filters",
    });
    await expect(filtersHeading).toBeVisible({ timeout: 10000 });

    const baseUrlText = page.getByText(/Showing 1 to \d+ of \d+/);
    let baseTotal = 0;
    try {
      const text = await baseUrlText.textContent();
      const match = text?.match(/of (\d+)/i);
      if (match) {
        baseTotal = parseInt(match[1], 10);
      }
    } catch {
      baseTotal = 0;
    }

    const sectionsToTest = [
      { columnId: "selectType", label: "select type" },
      { columnId: "radioType", label: "radio type" },
      { columnId: "ontologyLargeType", label: "ontology large type" },
      {
        columnId: "ontologySmallArrayType",
        label: "ontology small array type",
      },
      { columnId: "ontologySmallTreeType", label: "ontology small tree type" },
      {
        columnId: "showVisibleExpressionVariable",
        label:
          "If put to yes the test visible expression variable will be shown",
      },
    ];

    const results: {
      [key: string]: { expected: number; actual: number; clickedLabel: string };
    } = {};
    const clickedLabels: string[] = [];

    for (const section of sectionsToTest) {
      const sectionName = section.label;
      const sectionId = `filter-section-${section.columnId}`;

      const sectionHeading = page.locator("h3").filter({
        hasText: new RegExp(
          `^${sectionName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`
        ),
      });
      const exists = await sectionHeading.count().then((c) => c > 0);
      expect(exists, `section "${sectionName}" must be present`).toBe(true);

      await sectionHeading.first().scrollIntoViewIfNeeded();
      const isVisible = await sectionHeading
        .first()
        .isVisible()
        .catch(() => false);
      expect(
        isVisible,
        `section "${sectionName}" must be visible after scrolling`
      ).toBe(true);

      const sectionToggle = page.locator(`div[aria-controls="${sectionId}"]`);
      const toggleExists = await sectionToggle.count().then((c) => c > 0);
      expect(
        toggleExists,
        `toggle button for section "${sectionName}" must exist`
      ).toBe(true);

      const isExpanded =
        (await sectionToggle.first().getAttribute("aria-expanded")) === "true";
      if (!isExpanded) {
        await sectionToggle.first().click();
        await page.waitForTimeout(500);
      }

      const sectionContent = page.locator(`#${sectionId}`);
      const contentExists = await sectionContent.count().then((c) => c > 0);
      expect(
        contentExists,
        `section content container for "${sectionName}" must exist`
      ).toBe(true);

      const firstLabel = sectionContent
        .locator('label:has(input[type="checkbox"])')
        .first();
      const firstLabelVisible = await firstLabel.isVisible().catch(() => false);
      expect(
        firstLabelVisible,
        `section "${sectionName}" must have at least one filter option`
      ).toBe(true);

      const labelText = await firstLabel.textContent();
      const countMatch = labelText?.match(/\((\d+)\)$/);
      expect(
        countMatch,
        `section "${sectionName}" first option must have a count in format "(N)". Got: "${labelText}"`
      ).toBeTruthy();

      const expectedCount = parseInt(countMatch![1], 10);
      const cleanedLabel = labelText?.trim() || "unknown";
      results[sectionName] = {
        expected: expectedCount,
        actual: 0,
        clickedLabel: cleanedLabel,
      };
      clickedLabels.push(cleanedLabel);

      console.log(`[${section.columnId}] clicking: "${cleanedLabel}"`);

      await firstLabel.click();
      await page.waitForLoadState("networkidle", { timeout: 15000 });
      await page.waitForTimeout(1000);

      let actualCount = 0;
      const noDataMsg = page.getByText(/No data matched the filters/i);
      const isNoData = await noDataMsg.isVisible().catch(() => false);

      if (isNoData) {
        actualCount = 0;
      } else {
        const paginationText = page.getByText(/Showing 1 to \d+ of \d+/);
        const paginationVisible = await paginationText
          .isVisible()
          .catch(() => false);
        expect(
          paginationVisible,
          `pagination text must be visible after selecting filter in "${sectionName}"`
        ).toBe(true);
        const text = await paginationText.textContent();
        const match = text?.match(/of (\d+)/i);
        expect(
          match,
          `pagination text must contain "of N" format in "${sectionName}". Got: "${text}"`
        ).toBeTruthy();
        actualCount = parseInt(match![1], 10);
      }

      results[sectionName].actual = actualCount;
      expect(actualCount).toBe(
        expectedCount,
        `${sectionName}: expected ${expectedCount} but got ${actualCount}`
      );

      const clearBtn = page.getByRole("button", { name: /Clear all filters/i });
      const clearBtnVisible = await clearBtn.isVisible().catch(() => false);
      expect(
        clearBtnVisible,
        `Clear all filters button must be visible after selection in "${sectionName}"`
      ).toBe(true);

      await clearBtn.click();
      await page.waitForLoadState("networkidle", { timeout: 15000 });
      await page.waitForTimeout(1000);

      const finalTotalText = page.getByText(/Showing 1 to \d+ of \d+/);
      const finalTotalVisible = await finalTotalText
        .isVisible()
        .catch(() => false);
      expect(
        finalTotalVisible,
        `pagination text must be visible after clearing filters from "${sectionName}"`
      ).toBe(true);

      const text = await finalTotalText.textContent();
      const match = text?.match(/of (\d+)/i);
      expect(
        match,
        `pagination text must contain "of N" format after clearing from "${sectionName}". Got: "${text}"`
      ).toBeTruthy();
      const finalTotal = parseInt(match![1], 10);
      expect(finalTotal).toBe(
        baseTotal,
        `After clearing filters from "${sectionName}", expected ${baseTotal} but got ${finalTotal}`
      );
    }

    console.log("\nClicked labels by section:");
    for (const label of clickedLabels) {
      console.log(`  - ${label}`);
    }

    const uniqueLabels = new Set(clickedLabels);
    console.log(
      `\nUnique labels count: ${uniqueLabels.size} (expected: ${clickedLabels.length})`
    );
    if (uniqueLabels.size !== clickedLabels.length) {
      console.warn(
        "WARNING: Some sections clicked the same label - locator may still be broken"
      );
    }

    console.log("\nResults:");
    for (const [section, counts] of Object.entries(results)) {
      const pass = counts.expected === counts.actual ? "✓" : "✗";
      console.log(
        `${pass} ${section}: expected ${counts.expected}, got ${counts.actual} (clicked: "${counts.clickedLabel}")`
      );
    }
  });

  test("range filter (between) shape accepts numeric input without error", async ({
    page,
  }) => {
    const url = `${route}type%20test/Types?mg_filters=intType`;
    await page.goto(url);
    await page.waitForLoadState("networkidle", { timeout: 15000 });

    const filtersHeading = page.getByRole("heading", { level: 2 }).filter({
      hasText: "Filters",
    });
    const filtersVisible = await filtersHeading
      .isVisible({ timeout: 5000 })
      .catch(() => false);
    if (!filtersVisible) {
      test.skip(true, "Filters not available");
    }

    const paginationText = page.getByText(/Showing 1 to \d+ of \d+/);
    const paginationVisible = await paginationText
      .isVisible({ timeout: 10000 })
      .catch(() => false);

    expect(paginationVisible).toBe(
      true,
      "Pagination should be visible (range filter shape should not cause HTTP 400)"
    );

    const text = await paginationText.textContent();
    const match = text?.match(/of (\d+)/i);
    let totalCount = 0;
    if (match) {
      totalCount = parseInt(match[1], 10);
    }

    expect(totalCount).toBeGreaterThanOrEqual(0);
  });
});
