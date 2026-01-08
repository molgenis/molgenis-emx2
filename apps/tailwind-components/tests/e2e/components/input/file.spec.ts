import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe(
  "File input",
  {
    tag: "@tw-components @tw-forms @tw-file",
  },
  () => {
    test.beforeEach(async ({ page }) => {
      await page.goto(`${route}input/File.story`);
    });

    test("input is empty by default", async ({ page }) => {
      const inputValueContainer = page.locator(
        "div[data-elem='current-file-container']"
      );
      expect(
        (inputValueContainer as unknown as HTMLInputElement).children
      ).toBeFalsy();
    });
  }
);
