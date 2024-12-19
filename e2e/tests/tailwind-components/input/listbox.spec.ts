import { test, expect } from "@playwright/test";

test(
  "Listbox: button has correct aria attributes @tw-components @tw-forms @tw-listbox",
  async({ page }) => {
    await page.goto("/apps/tailwind-components/#/input/Listbox.story");
    const combobox = await page.getByRole("combobox");
    await expect(combobox).toHaveAttribute("aria-required");
    await expect(combobox).toHaveAttribute("aria-expanded");
    await expect(combobox).toHaveAttribute("aria-labelledby");
    await expect(combobox).toHaveAttribute("role", "combobox");
    await expect(combobox).toHaveAttribute("aria-haspopup", "listbox");
  }
);

test(
  "Listbox: combox is properly linked to the listbox @tw-components @tw-forms @tw-listbox",
  async ({ page }) => {
    await page.goto("/apps/tailwind-components/#/input/Listbox.story");
    const combobox = await page.getByRole("combobox");
    const listbox = await page.getByRole("listbox");
    await combobox.click();
    
    const comboxId = await combobox.getAttribute("aria-controls");
    const listboxId = await listbox.getAttribute("id");
    await expect(comboxId).toEqual(listboxId);   
  }
)

test(
  "Listbox: listbox has is properly labelled @tw-components @tw-forms @tw-listbox",
  async ({ page }) => {
    await page.goto("/apps/tailwind-components/#/input/Listbox.story");
    const combobox = await page.getByRole("combobox");
    const comboxId = await combobox.getAttribute("aria-labelledby");
    await page.locator(`label[for='${comboxId}']`);
  }
)

test(
  "Listbox: aria expand properties correctly update when expanded @tw-components @tw-forms @tw-listbox",
  async({ page }) => {
    await page.goto("/apps/tailwind-components/#/input/Listbox.story");
    const combobox = await page.getByRole("combobox");
    await expect(combobox).toHaveAttribute("aria-expanded", "false");
  }
);

test(
  "Listbox: placeholder is the first element in the list @tw-components @tw-forms @tw-listbox",
  async ({ page }) => {
    await page.goto("/apps/tailwind-components/#/input/Listbox.story");
    const combobox = await page.getByRole("combobox");
    await combobox.click();
    const option = await page.getByRole("option").first();
    await expect(option).toHaveText("Select an option");
  }
)

test(
  "Listbox: list items have the proper attributes @tw-components @tw-forms @tw-listbox",
  async ({ page }) => {
    await page.goto("/apps/tailwind-components/#/input/Listbox.story");
    const combobox = await page.getByRole("combobox");
    combobox.click();
    await expect(combobox).toHaveAttribute("aria-expanded", "true");
    for (const option of await page.getByRole("option").all()) { 
      if (await option.innerText() === "Select an option") {
        await expect(option).toHaveAttribute("aria-selected", "true");
      } else {
        await expect(option).toHaveAttribute("aria-selected", "false");
      }
    }
  }
)
