import { test, expect } from "@playwright/test";

test("Regression: URL vs Interactive filter application", async ({ page }) => {
  const baseUrl = "http://localhost:3000";

  // Scenario 1: Load via URL with pre-applied filter
  console.log("\n=== SCENARIO 1: URL-LOAD ===");
  const urlWithFilter = `${baseUrl}/catalogue-demo/Resources?mg_filters=hricore,countries,continents,fundingSources,fundingScheme,applicableLegislation,id&continents.0=Oceania`;

  await page.goto(urlWithFilter, { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2000);

  const urlLoadScreenshot = "/tmp/url_load.png";
  await page.screenshot({ path: urlLoadScreenshot, fullPage: true });
  console.log(`✓ URL-load screenshot saved: ${urlLoadScreenshot}`);

  // Capture visible filter sections from URL load
  const urlLoadSnapshot = await captureFilterSnapshot(page);
  console.log("Visible filters in URL scenario:");
  logFilterSnapshot(urlLoadSnapshot);

  // Check for zero-count visible options
  const zeroCountElements = await page
    .locator("text=/\\(0\\)/")
    .allTextContents();
  if (zeroCountElements.length > 0) {
    console.log(
      `⚠️ Found ${zeroCountElements.length} elements with (0) count visible:`
    );
    zeroCountElements
      .slice(0, 5)
      .forEach((t) => console.log(`   - ${t.trim()}`));
    if (zeroCountElements.length > 5) {
      console.log(`   ... and ${zeroCountElements.length - 5} more`);
    }
  } else {
    console.log("✓ No (0) count visible options found");
  }

  // Scenario 2: Load page fresh, then apply filter interactively
  console.log("\n=== SCENARIO 2: INTERACTIVE ===");

  await page.goto(`${baseUrl}/catalogue-demo/Resources/`, {
    waitUntil: "networkidle",
    timeout: 30000,
  });
  await page.waitForTimeout(2000);

  // Find Continents section and expand if needed
  const continentsSectionHeader = page.locator('h3:has-text("Continents")');
  await expect(continentsSectionHeader).toBeVisible({ timeout: 10000 });

  // Click on Oceania - try multiple selectors
  let oceaniaClicked = false;

  // Try finding the tree item containing Oceania
  const treeItems = page.locator('[role="treeitem"]');
  const count = await treeItems.count();

  for (let i = 0; i < count; i++) {
    const item = treeItems.nth(i);
    try {
      const text = await item.textContent();
      if (text?.includes("Oceania") && !oceaniaClicked) {
        await item.click();
        console.log(`✓ Clicked Oceania (found in tree)`);
        oceaniaClicked = true;
        break;
      }
    } catch (e) {
      // continue
    }
  }

  if (!oceaniaClicked) {
    // Try direct button/label click
    const oceaniaBtn = page.locator('button:has-text("Oceania")');
    if (
      await oceaniaBtn
        .first()
        .isVisible({ timeout: 2000 })
        .catch(() => false)
    ) {
      await oceaniaBtn.first().click();
      console.log("✓ Clicked Oceania (button)");
      oceaniaClicked = true;
    }
  }

  if (oceaniaClicked) {
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1000);

    const interactiveScreenshot = "/tmp/interactive.png";
    await page.screenshot({ path: interactiveScreenshot, fullPage: true });
    console.log(`✓ Interactive screenshot saved: ${interactiveScreenshot}`);

    const interactiveSnapshot = await captureFilterSnapshot(page);
    console.log("Visible filters in interactive scenario:");
    logFilterSnapshot(interactiveSnapshot);

    // Compare
    console.log("\n=== COMPARISON ===");
    diffSnapshots(urlLoadSnapshot, interactiveSnapshot);
  } else {
    console.log(
      "⚠️ Could not locate and click Oceania - this may be expected if it's not visible initially"
    );
  }
});

async function captureFilterSnapshot(page): Promise<Record<string, string[]>> {
  const snapshot: Record<string, string[]> = {};

  // Get all section headers
  const sections = await page.locator("h3").allTextContents();
  for (const sectionName of sections) {
    const clean = sectionName.trim();
    if (clean) snapshot[clean] = [];
  }

  // Get all tree items visible in the tree
  const treeItems = page.locator('[role="treeitem"]');
  const itemCount = await treeItems.count();

  const allItemTexts: string[] = [];
  for (let i = 0; i < itemCount; i++) {
    try {
      const text = await treeItems.nth(i).textContent();
      if (text?.trim()) {
        allItemTexts.push(text.trim());
      }
    } catch (e) {
      // skip unavailable items
    }
  }

  // Simple grouping: items appearing in order after a section header
  let currentSection = "";
  for (const item of allItemTexts) {
    const sectionMatch = Object.keys(snapshot).find((s) => item.includes(s));
    if (sectionMatch && item.length < 50) {
      currentSection = sectionMatch;
    } else if (currentSection && item.length < 100) {
      snapshot[currentSection].push(item);
    }
  }

  return snapshot;
}

function logFilterSnapshot(snapshot: Record<string, string[]>) {
  for (const [section, options] of Object.entries(snapshot)) {
    if (options.length === 0) {
      console.log(`  [${section}] - (no visible options)`);
    } else {
      console.log(`  [${section}]`);
      const uniqueOpts = [...new Set(options)];
      uniqueOpts.slice(0, 8).forEach((opt) => console.log(`    - ${opt}`));
      if (uniqueOpts.length > 8) {
        console.log(`    ... (${uniqueOpts.length - 8} more)`);
      }
    }
  }
}

function diffSnapshots(
  url: Record<string, string[]>,
  interactive: Record<string, string[]>
) {
  let hasDiff = false;
  for (const section of Object.keys(url)) {
    const urlSet = new Set(url[section]);
    const intSet = new Set(interactive[section] || []);

    const onlyUrl = [...urlSet].filter((o) => !intSet.has(o));
    const onlyInt = [...intSet].filter((o) => !urlSet.has(o));

    if (onlyUrl.length > 0 || onlyInt.length > 0) {
      hasDiff = true;
      console.log(`\n  [${section}]`);
      if (onlyUrl.length > 0) {
        console.log(`    ⚠️ Only in URL load:`);
        onlyUrl.slice(0, 5).forEach((o) => console.log(`      - ${o}`));
        if (onlyUrl.length > 5)
          console.log(`      ... (${onlyUrl.length - 5} more)`);
      }
      if (onlyInt.length > 0) {
        console.log(`    ℹ️ Only in interactive:`);
        onlyInt.slice(0, 5).forEach((o) => console.log(`      - ${o}`));
        if (onlyInt.length > 5)
          console.log(`      ... (${onlyInt.length - 5} more)`);
      }
    }
  }
  if (!hasDiff) {
    console.log("✓ No differences detected");
  }
}
