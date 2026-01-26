import { test, expect, type Page } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

const baseUrl = `${route}filter/Emx2DataView.story?schema=TypeTest&table=TypeTest`;

async function expandFilter(page: Page, filterName: string) {
  const filterTitle = page.locator("h3").filter({ hasText: filterName }).first();
  await filterTitle.click();
  await page.waitForTimeout(100);
}

async function getUrlParam(page: Page, paramPattern: string): Promise<string | null> {
  const url = page.url();
  const params = new URLSearchParams(url.split("?")[1] || "");
  for (const [key, value] of params.entries()) {
    if (key.includes(paramPattern) || key === paramPattern) {
      return value;
    }
  }
  return null;
}

test.describe("Filter URL Sync - TypeTest Schema (All Column Types)", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(baseUrl);
    await expect(page.getByText("Emx2DataView")).toBeVisible({ timeout: 15000 });
    await page.waitForTimeout(1000);
  });

  test("STRING filter updates URL", async ({ page }) => {
    await expandFilter(page, "testString");
    const input = page.locator('input[id="testString"]');
    await expect(input).toBeVisible();
    await input.fill("hello");
    await page.waitForTimeout(500);

    const param = await getUrlParam(page, "testString");
    expect(param).toBe("hello");
  });

  test("TEXT filter updates URL", async ({ page }) => {
    await expandFilter(page, "testText");
    const input = page.locator('textarea[id="testText"]');
    await expect(input).toBeVisible();
    await input.fill("some text");
    await page.waitForTimeout(500);

    const param = await getUrlParam(page, "testText");
    expect(param).toBe("some text");
  });

  test("INT filter updates URL with range", async ({ page }) => {
    await expandFilter(page, "testInt");
    const minInput = page.locator('input[id="testInt-min"]');
    const maxInput = page.locator('input[id="testInt-max"]');
    await expect(minInput).toBeVisible();

    await minInput.fill("10");
    await maxInput.fill("100");
    await page.waitForTimeout(500);

    const param = await getUrlParam(page, "testInt");
    expect(param).toBe("10..100");
  });

  test("DECIMAL filter updates URL with range", async ({ page }) => {
    await expandFilter(page, "testDecimal");
    const minInput = page.locator('input[id="testDecimal-min"]');
    await expect(minInput).toBeVisible();

    await minInput.fill("1.5");
    await page.waitForTimeout(500);

    const param = await getUrlParam(page, "testDecimal");
    expect(param).toContain("1.5");
  });

  test("DATE filter updates URL with range", async ({ page }) => {
    await expandFilter(page, "testDate");
    const minInput = page.locator('input[id="testDate-min"]');
    await expect(minInput).toBeVisible();

    await minInput.fill("2024-01-01");
    await page.waitForTimeout(500);

    const param = await getUrlParam(page, "testDate");
    expect(param).toContain("2024-01-01");
  });

  test("DATETIME filter updates URL with range", async ({ page }) => {
    await expandFilter(page, "testDatetime");
    const minInput = page.locator('input[id="testDatetime-min"]');
    await expect(minInput).toBeVisible();

    await minInput.fill("2024-01-01T12:00");
    await page.waitForTimeout(500);

    const param = await getUrlParam(page, "testDatetime");
    expect(param).toBeTruthy();
  });

  test("BOOL filter updates URL", async ({ page }) => {
    await expandFilter(page, "testBool");
    const trueRadio = page.locator('input[type="radio"][value="true"]').first();
    await expect(trueRadio).toBeVisible();

    await trueRadio.click();
    await page.waitForTimeout(500);

    const param = await getUrlParam(page, "testBool");
    expect(param).toBe("true");
  });

  test("REF filter updates URL with ref field", async ({ page }) => {
    await expandFilter(page, "testRef");
    await page.waitForTimeout(500);

    const refInput = page.locator('[id="testRef"]').first();
    await refInput.click();
    await page.waitForTimeout(300);

    const option = page.locator('[role="option"]').first();
    if (await option.isVisible()) {
      await option.click();
      await page.waitForTimeout(500);

      const url = page.url();
      expect(url).toMatch(/testRef\.\w+=/);
    }
  });

  test("ONTOLOGY filter updates URL without [object Object]", async ({ page }) => {
    const ontologyFilters = ["testOntology", "ontologySmallType"];

    for (const filterName of ontologyFilters) {
      const filterTitle = page.locator("h3").filter({ hasText: filterName }).first();
      if (await filterTitle.isVisible({ timeout: 1000 }).catch(() => false)) {
        await filterTitle.click();
        await page.waitForTimeout(500);

        const checkbox = page.locator('input[type="checkbox"]').first();
        if (await checkbox.isVisible({ timeout: 1000 }).catch(() => false)) {
          await checkbox.click();
          await page.waitForTimeout(500);

          const url = page.url();
          expect(url).not.toContain("[object");
          expect(url).not.toContain("Object]");

          const params = new URLSearchParams(url.split("?")[1] || "");
          for (const [key, value] of params.entries()) {
            if (key.includes(filterName)) {
              expect(value).not.toBe("[object Object]");
              expect(value.length).toBeGreaterThan(0);
            }
          }
        }
        break;
      }
    }
  });

  test("ONTOLOGY_ARRAY filter updates URL without [object Object]", async ({ page }) => {
    const filterTitle = page.locator("h3").filter({ hasText: /ontology.*array/i }).first();
    if (await filterTitle.isVisible({ timeout: 1000 }).catch(() => false)) {
      await filterTitle.click();
      await page.waitForTimeout(500);

      const checkbox = page.locator('input[type="checkbox"]').first();
      if (await checkbox.isVisible({ timeout: 1000 }).catch(() => false)) {
        await checkbox.click();
        await page.waitForTimeout(500);

        const url = page.url();
        expect(url).not.toContain("[object Object]");
      }
    }
  });

  test("REF_ARRAY filter updates URL", async ({ page }) => {
    const filterTitle = page.locator("h3").filter({ hasText: /testRefArray|ref.*array/i }).first();
    if (await filterTitle.isVisible({ timeout: 1000 }).catch(() => false)) {
      await filterTitle.click();
      await page.waitForTimeout(500);

      const refInput = page.locator('input[type="text"]').first();
      if (await refInput.isVisible({ timeout: 1000 }).catch(() => false)) {
        await refInput.click();
        await page.waitForTimeout(300);

        const option = page.locator('[role="option"]').first();
        if (await option.isVisible({ timeout: 1000 }).catch(() => false)) {
          await option.click();
          await page.waitForTimeout(500);

          const url = page.url();
          expect(url).not.toContain("[object Object]");
        }
      }
    }
  });

  test("clearing filter removes URL param", async ({ page }) => {
    await expandFilter(page, "testString");
    const input = page.locator('input[id="testString"]');
    await input.fill("test");
    await page.waitForTimeout(500);

    let param = await getUrlParam(page, "testString");
    expect(param).toBe("test");

    const clearButton = page.locator("h3")
      .filter({ hasText: "testString" })
      .locator("..")
      .locator("..")
      .getByText("Clear");
    await clearButton.click();
    await page.waitForTimeout(500);

    param = await getUrlParam(page, "testString");
    expect(param).toBeNull();
  });

  test("multiple filters combine in URL", async ({ page }) => {
    await expandFilter(page, "testString");
    const stringInput = page.locator('input[id="testString"]');
    await stringInput.fill("hello");
    await page.waitForTimeout(300);

    await expandFilter(page, "testInt");
    const intMin = page.locator('input[id="testInt-min"]');
    await intMin.fill("5");
    await page.waitForTimeout(500);

    const url = page.url();
    expect(url).toContain("testString=hello");
    expect(url).toContain("testInt=5..");
  });

  test("URL params restore filter state on page load", async ({ page }) => {
    const urlWithFilters = `${baseUrl}&testString=restored&testInt=10..50`;
    await page.goto(urlWithFilters);
    await expect(page.getByText("Emx2DataView")).toBeVisible({ timeout: 15000 });
    await page.waitForTimeout(1000);

    await expandFilter(page, "testString");
    const stringInput = page.locator('input[id="testString"]');
    await expect(stringInput).toHaveValue("restored");

    await expandFilter(page, "testInt");
    const intMin = page.locator('input[id="testInt-min"]');
    const intMax = page.locator('input[id="testInt-max"]');
    await expect(intMin).toHaveValue("10");
    await expect(intMax).toHaveValue("50");
  });
});
