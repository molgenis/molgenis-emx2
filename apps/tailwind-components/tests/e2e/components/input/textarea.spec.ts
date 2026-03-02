import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}input/TextArea.story`);
  await page
    .getByRole("heading", { name: "InputTextArea" })
    .click({ delay: 1000 });
});

test("InputTextArea: invalid is properly indicated @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page
    .locator("label")
    .filter({ hasText: "invalid" })
    .locator("rect")
    .click();
  const inputTextArea = page.locator("#story-input-text-area-1");
  await expect(inputTextArea).toBeVisible();
  const inputTextAreaClass = await inputTextArea.getAttribute("class");
  await expect(inputTextAreaClass).toContain("border-invalid text-invalid");
});

test("InputTextArea: valid state properly styles component @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page.getByText("valid", { exact: true }).click();
  const inputTextArea = page.locator("#story-input-text-area-1");
  await expect(inputTextArea).toBeVisible();
  const inputTextAreaClass = await inputTextArea.getAttribute("class");
  await expect(inputTextAreaClass).toContain("border-valid text-valid");
});

test("InputTextArea: component is properly disabled @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page.getByText("disabled").click();
  const inputTextArea = page.locator("#story-input-text-area-1");
  await expect(inputTextArea).toBeDisabled();
});

test("InputTextArea: component properly displays placeholder @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page.getByRole("textbox", { name: "Placeholder" }).clear();
  await page
    .getByRole("textbox", { name: "Placeholder" })
    .fill("This is a new placeholder for the textarea component");
  await expect(page.locator("#story-input-text-area-1")).toHaveAttribute(
    "placeholder",
    "This is a new placeholder for the textarea component"
  );
});
