// import { test, expect } from "@playwright/test";
// import playwrightConfig from "../../../playwright.config";

// const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
//   ? ""
//   : "/apps/tailwind-components/#/";

// test.beforeEach(async ({ page }) => {
//   await page.goto(`${route}input/Checkbox.story`);
//   await page
//     .getByText("InputCheckbox", { exact: true })
//     .first()
//     .click({ delay: 300 });
// });

// test("Checkbox input: labels are properly displayed @tw-components @tw-forms @tw-checkbox", async ({
//   page,
// }) => {
//   await expect(
//     page.locator("label").filter({ hasText: "Would you like to subscribe" })
//   ).toHaveText("Would you like to subscribe to our newsletter?");
// });

// test("Checkbox input: svg icon updates when checked @tw-components @tw-forms @tw-checkbox", async ({
//   page,
// }) => {
//   await page.getByText("Would you like to subscribe").click();
//   await expect(
//     page
//       .locator("label")
//       .filter({ hasText: "Would you like to subscribe" })
//       .getByRole("img")
//   ).toHaveAttribute("data-checked", "true");
//   await page.getByText("Would you like to subscribe").click();
//   await expect(
//     page
//       .locator("label")
//       .filter({ hasText: "Would you like to subscribe" })
//       .getByRole("img")
//   ).toHaveAttribute("data-checked", "false");
// });

// test("Checkbox input: input values are properly binded to the input element @tw-components @tw-forms @tw-checkbox", async ({
//   page,
// }) => {
//   await page.getByText("Do you agree to the terms and").click();
//   await expect(page.getByText("Do you agree to the terms and")).toBeChecked();
// });

// test("Checkbox group input: default options are checked @tw-components @tw-forms @tw-checkbox", async ({
//   page,
// }) => {
//   await expect(page.getByText("Roma tomatoes")).toBeChecked();
//   await expect(page.getByText("Pepperoni")).toBeChecked({ checked: false });
//   await expect(page.getByText("Fresh mozzerella")).toBeChecked({
//     checked: false,
//   });
//   await expect(page.getByText("Chillies")).toBeChecked({ checked: false });
//   await expect(page.getByText("Fresh basil")).toBeChecked();
// });
