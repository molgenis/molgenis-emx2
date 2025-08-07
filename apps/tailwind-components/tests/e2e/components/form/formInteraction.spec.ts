import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe(
  "Form Interaction",
  {
    tag: "@tw-components @tw-forms",
  },
  () => {
    test.beforeEach(async ({ page }) => {
      await page.goto(`${route}Form.story?schema=pet+store&table=Pet`);
      await page.getByText("Jump to", { exact: true }).click({ delay: 300 });
    });

    test("it should update the model value when a field is filled out", async ({
      page,
    }) => {
      await page.getByLabel("name Required", { exact: true }).click();
      await page.getByLabel("name Required", { exact: true }).fill("test");
      await expect(
        page.getByLabel("name Required", { exact: true })
      ).toHaveValue("test");
    });

    test("it should not jump around when selecting a checkbox", async ({
      page,
    }) => {
      await page.goto(`${route}Form.story?schema=pet+store&table=User`);
      await page.evaluate(() => location.reload()); //help nuxt
      await page.getByText("username", { exact: true }).waitFor();

      //scroll into view
      await page.getByText("Show source code").scrollIntoViewIfNeeded();

      // select checkbox inputs
      await page.locator("label").filter({ hasText: "pooky" }).click();
      await page.locator("label").filter({ hasText: "spike" }).click();

      // determine if filter well buttons are visible (i.e., component has selection)
      await expect(page.getByRole("button", { name: "pooky" })).toBeVisible();
      await expect(page.getByRole("button", { name: "spike" })).toBeVisible();
    });
  }
);
