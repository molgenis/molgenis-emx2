import { test, expect } from "@playwright/test";
import playwrightConfig from "~/playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}FormField.story`);
  await page.getByRole("heading", { name: "FormField" }).click({ delay: 300 });
});

test("InputTextArea: invalid is properly indicated @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await expect(page.locator("#test-state-checkbox-group")).toContainText(
    "invalid"
  );
  await page
    .locator("label")
    .filter({ hasText: "invalid" })
    .locator("rect")
    .click();
  await page.getByLabel("invalid").check();
  await expect(page.getByLabel("invalid")).toBeChecked();
  const InputTextAreaClass = await page
    .getByLabel("Demo input for type=text")
    .getAttribute("class");
  await expect(InputTextAreaClass).toContain("invalid");
});

test("InputTextArea: required state is properly indicated @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page.getByText("Required is true").click();
  await expect(
    page.getByLabel("Demo input for type=text Required")
  ).toBeVisible();
});

test("InputTextArea: valid state properly styles component @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page
    .locator("label")
    .filter({ hasText: /^valid$/ })
    .locator("rect")
    .click();
  const InputTextAreaClass = await page
    .getByLabel("Demo input for type=text")
    .getAttribute("class");
  await expect(InputTextAreaClass).toContain("border-valid text-valid");
});

test("InputTextArea: component is properly disabled @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page.getByText("disabled").click();
  await expect(page.getByLabel("Demo input for type=text")).toBeDisabled();
});

test("InputTextArea: component properly displays placeholder @tw-components @tw-forms @input-textarea", async ({
  page,
}) => {
  await page.getByLabel("Demo input for type=text").clear();
  await page
    .getByLabel("Placeholder")
    .fill("This is a new placeholder for the textarea component");
  await expect(page.getByLabel("Placeholder")).toHaveValue(
    "This is a new placeholder for the textarea component"
  );
  await expect(page.getByLabel("Demo input for type=text")).toHaveAttribute(
    "placeholder"
  );
});
