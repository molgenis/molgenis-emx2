import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.describe(
  "Listbox component",
  {
    tag: "@tw-components @tw-forms @tw-listbox",
  },
  () => {
    test.beforeEach(async ({ page }) => {
      await page.goto(`${route}input/Listbox.story`);
      await page
        .getByText("Listbox component", { exact: true })
        .first()
        .click({ delay: 300 });
    });

    test(
      "down key opens listbox",
      {
        tag: "@keyboard-events",
      },
      async ({ page }) => {
        const combobox = await page.getByRole("combobox");
        const listbox = await page.getByRole("listbox");

        await combobox.focus();
        await page.keyboard.press("ArrowDown");
        await expect(combobox).toHaveAttribute("aria-expanded", "true");
        await expect(listbox).toHaveAttribute("aria-expanded", "true");
      }
    );

    test(
      "enter key opens and closes listbox",
      {
        tag: "@keyboard-events",
      },
      async ({ page }) => {
        const combobox = await page.getByRole("combobox");
        const listbox = await page.getByRole("listbox");

        await combobox.focus();
        await page.keyboard.press("Enter");
        await expect(combobox).toHaveAttribute("aria-expanded", "true");
        await expect(listbox).toHaveAttribute("aria-expanded", "true");

        await page.keyboard.press("Enter");
        await expect(combobox).toHaveAttribute("aria-expanded", "false");
        await expect(listbox).toBeHidden();
      }
    );

    test(
      "space key opens and closes listbox",
      {
        tag: "@keyboard-events",
      },
      async ({ page }) => {
        const combobox = await page.getByRole("combobox");
        const listbox = await page.getByRole("listbox");

        await combobox.focus();
        await page.keyboard.press("Space");
        await expect(combobox).toHaveAttribute("aria-expanded", "true");
        await expect(listbox).toHaveAttribute("aria-expanded", "true");

        await page.keyboard.press("Space");
        await expect(combobox).toHaveAttribute("aria-expanded", "false");
        await expect(listbox).toBeHidden();
      }
    );

    test(
      "End and home buttons focuses the last and first options",
      {
        tag: "@keyboard-events",
      },
      async ({ page }) => {
        const combobox = await page.getByRole("combobox");

        await combobox.focus();
        await page.keyboard.press("Enter");
        await page.keyboard.press("End");

        const lastOption = await page.getByRole("option").last();
        await expect(lastOption).toBeFocused();

        await page.keyboard.press("Home");
        const firstOption = await page.getByRole("option").first();
        await expect(firstOption).toBeFocused();
      }
    );

    test(
      "When opened, arrow keys properly focus and select items",
      {
        tag: "@keyboard-events",
      },
      async ({ page }) => {
        const combobox = await page.getByRole("combobox");

        await combobox.focus();
        await page.keyboard.press("Enter");
        await page.keyboard.press("ArrowDown");
        const secondOption = await page.getByRole("option").nth(1);
        await expect(secondOption).toBeFocused();
      }
    );
  }
);
