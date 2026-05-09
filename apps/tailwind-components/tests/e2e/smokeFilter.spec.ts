import { test, expect, type Request } from "@playwright/test";

test.describe("filter sidebar smoke + H13 — real backend", () => {
  let gqlRequests: { url: string; body: any; postData: string }[] = [];

  test.beforeEach(async ({ page }) => {
    gqlRequests = [];
    page.on("request", (req: Request) => {
      if (req.method() === "POST" && req.url().includes("/graphql")) {
        try {
          const postData = req.postData() ?? "{}";
          const body = JSON.parse(postData);
          gqlRequests.push({ url: req.url(), body, postData });
        } catch {}
      }
    });
  });

  test("probe: catalogue-demo tables for filters", async ({ page }) => {
    const tablesToTry = [
      "Resources",
      "Collections",
      "Variables",
      "Datasets",
      "Publications",
      "Networks",
      "Organisations",
    ];

    for (const table of tablesToTry) {
      await page.goto(`/catalogue-demo/${table}/`);
      await page.waitForLoadState("networkidle");

      // Check for checkboxes, inputs, and filter sidebar
      const checkboxes = await page.locator('input[type="checkbox"]').count();
      const textInputs = await page
        .locator('input[type="text"], input[type="search"]')
        .count();
      const sidebarText = await page.locator('h2:has-text("Filters")').count();

      console.log(
        `TABLE: ${table} | Checkboxes: ${checkboxes}, TextInputs: ${textInputs}, FilterSidebar: ${sidebarText}`
      );

      if (checkboxes > 0 || textInputs > 0) {
        console.log(`  >>> TABLE ${table} HAS FILTERS`);
      }
    }
  });

  test("H13: text filter typing sends clean GraphQL, no empty filters", async ({
    page,
  }) => {
    await page.goto("/catalogue-demo/Resources/");
    await page.waitForLoadState("networkidle");

    // Find a text/search input in the filter sidebar
    const textInputs = page
      .locator('input[type="text"], input[type="search"]')
      .all();
    const inputs = await textInputs;

    if (inputs.length === 0) {
      console.log("NO_TEXT_INPUTS_FOUND_ON_PAGE");
      expect(inputs.length).toBeGreaterThan(0);
    }

    const targetInput = inputs[0];
    const placeholder = await targetInput.getAttribute("placeholder");
    console.log(`USING_INPUT_PLACEHOLDER: ${placeholder || "(none)"}`);

    // Clear captured requests before test
    gqlRequests = [];

    // Type slowly: "smith" at ~1 char per 200ms
    await targetInput.focus();
    for (const char of "smith") {
      await targetInput.type(char);
      await page.waitForTimeout(200);
    }

    console.log(`AFTER_TYPING_SMITH: ${gqlRequests.length} GQL requests`);
    for (let i = 0; i < Math.min(2, gqlRequests.length); i++) {
      const req = gqlRequests[i];
      console.log(
        `GQL_DURING_TYPE[${i}]: ${req.postData.substring(0, 300)}...`
      );
    }

    // Wait for debounce
    await page.waitForTimeout(700);

    const countAfterType = gqlRequests.length;
    console.log(`AFTER_DEBOUNCE: ${countAfterType} total requests`);

    // Log the request that includes "smith"
    for (let i = 0; i < countAfterType; i++) {
      if (gqlRequests[i].postData.includes("smith")) {
        console.log(
          `GQL_WITH_SMITH[${i}]: ${gqlRequests[i].postData.substring(
            0,
            400
          )}...`
        );
        break;
      }
    }

    // Clear the input via input.clear() (which triggers input event)
    await targetInput.clear();
    await page.waitForTimeout(700);

    const countAfterClear = gqlRequests.length;
    console.log(`AFTER_CLEAR: ${countAfterClear} total requests`);

    // Log requests added by clear
    if (countAfterClear > countAfterType) {
      for (
        let i = countAfterType;
        i < Math.min(countAfterType + 2, countAfterClear);
        i++
      ) {
        const req = gqlRequests[i];
        const hasSmith = req.postData.includes("smith");
        console.log(
          `GQL_AFTER_CLEAR[${i}] (hasSmith=${hasSmith}): ${req.postData.substring(
            0,
            300
          )}...`
        );
      }
    }

    // Type "smith" again - DO NOT wait for debounce before checking
    await targetInput.type("smith", { delay: 50 });
    // Wait a bit but don't go past debounce
    await page.waitForTimeout(200);

    const countAfterRetype = gqlRequests.length;
    console.log(
      `AFTER_RETYPE_BEFORE_DEBOUNCE: ${countAfterRetype} total requests`
    );

    // Now wait for debounce
    await page.waitForTimeout(700);

    const countFinal = gqlRequests.length;
    console.log(`FINAL_COUNT: ${countFinal} total requests`);

    // Assertions: no empty filters, no "smith" in requests SENT AFTER clear (requests countAfterType onwards)
    let foundEmptyFilter = false;
    let foundSmithImmediatelyAfterClear = false;

    for (let i = 0; i < countAfterClear; i++) {
      const req = gqlRequests[i];
      const postData = req.postData;

      if (
        postData.includes('_like: ""') ||
        postData.includes('"_like": ""') ||
        postData.includes("_like: null") ||
        postData.includes('"_like": null')
      ) {
        foundEmptyFilter = true;
        console.log(
          `ERROR_EMPTY_FILTER_IN_REQ[${i}]: ${postData.substring(0, 400)}`
        );
      }

      // If this is a request sent AFTER clear started, it should NOT have "smith"
      if (i >= countAfterType && postData.includes('search:"smith"')) {
        foundSmithImmediatelyAfterClear = true;
        console.log(
          `ERROR_SMITH_IN_FIRST_CLEAR_REQ[${i}]: ${postData.substring(0, 400)}`
        );
      }
    }

    expect(foundEmptyFilter).toBe(
      false,
      "H13 FAIL: Found empty _like filter (empty string or null)"
    );
    expect(foundSmithImmediatelyAfterClear).toBe(
      false,
      "H13 FAIL: Found 'smith' in first request(s) sent after clear input"
    );
    console.log(
      "H13_PASS: Text filter typing/clearing generated clean GraphQL"
    );
  });

  test("smoke: search filter updates URL and triggers GQL", async ({
    page,
  }) => {
    await page.goto("/catalogue-demo/Resources/");
    await page.waitForLoadState("networkidle");

    const initialUrl = page.url();
    console.log(`INITIAL_URL: ${initialUrl}`);

    // Find text input
    const textInput = page
      .locator('input[type="text"], input[type="search"]')
      .first();
    const exists = await textInput.count();

    if (exists === 0) {
      console.log("NO_TEXT_INPUT_FOUND_SKIP_TEST");
      return;
    }

    gqlRequests = [];
    await textInput.focus();
    await textInput.type("test");
    await page.waitForTimeout(700);

    const urlAfterType = page.url();
    console.log(`URL_AFTER_TYPE: ${urlAfterType}`);
    console.log(`GQL_REQUESTS_AFTER_TYPE: ${gqlRequests.length}`);

    expect(urlAfterType).not.toEqual(
      initialUrl,
      "SMOKE_FAIL: URL did not change after typing"
    );
    expect(gqlRequests.length).toBeGreaterThan(
      0,
      "SMOKE_FAIL: No GraphQL requests after search"
    );
    console.log("SMOKE_PASS: Search filter updates URL and triggers GraphQL");
  });

  test("smoke: input clear removes search from URL", async ({ page }) => {
    await page.goto("/catalogue-demo/Resources/");
    await page.waitForLoadState("networkidle");

    // Type in search to create an active filter
    const textInput = page
      .locator('input[type="text"], input[type="search"]')
      .first();
    const exists = await textInput.count();

    if (exists === 0) {
      console.log("NO_TEXT_INPUT_SKIP_CLEAR_TEST");
      return;
    }

    await textInput.focus();
    await textInput.type("test");
    await page.waitForTimeout(700);

    const urlWithSearch = page.url();
    console.log(`URL_WITH_SEARCH: ${urlWithSearch}`);

    expect(urlWithSearch).toContain(
      "mg_search=test",
      "URL should have mg_search param"
    );

    // Now clear the input by selecting all and deleting
    gqlRequests = [];
    await textInput.clear();
    await page.waitForTimeout(700);

    const urlAfterClear = page.url();
    console.log(`URL_AFTER_CLEAR: ${urlAfterClear}`);
    console.log(`GQL_REQUESTS_AFTER_CLEAR: ${gqlRequests.length}`);

    // Check that mg_search is removed from URL
    const hasMgSearch = urlAfterClear.includes("mg_search");
    console.log(`URL_STILL_HAS_MG_SEARCH: ${hasMgSearch}`);

    expect(hasMgSearch).toBe(
      false,
      "SMOKE: URL should not contain mg_search after clearing input"
    );
    console.log("SMOKE_PASS: Clearing input removes search from URL");
  });
});
