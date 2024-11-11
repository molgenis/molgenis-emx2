import { test, expect } from "@playwright/test";

test("InputTextArea: error is properly indicated @tw-components @tw-forms @input-textarea", async ({ page }) => {
    await page.goto("/apps/tailwind-components/#/input/TextArea.story");
    await page.getByLabel('Show error').check(); 
    const InputTextAreaClass = await page.getByPlaceholder("This is placeholder text").getAttribute("class");
    await expect(InputTextAreaClass).toContain("border-invalid text-invalid");
});

test("InputTextArea: required state is properly indicated @tw-components @tw-forms @input-textarea", async ({ page}) => {
  await page.goto("/apps/tailwind-components/#/input/TextArea.story");
  await page.getByLabel('Required').check(); 
  await expect(page.getByPlaceholder("This is placeholder text")).toHaveAttribute("required");
})

test("InputTextArea: valid state properly styles component @tw-components @tw-forms @input-textarea", async ({page}) => {
  await page.goto("/apps/tailwind-components/#/input/TextArea.story");
  await page.getByLabel('Validate input').check();
  const InputTextAreaClass = await page.getByPlaceholder("This is placeholder text").getAttribute("class");
  await expect(InputTextAreaClass).toContain("border-valid text-valid");
})

test("InputTextArea: component is properly disabled @tw-components @tw-forms @input-textarea", async ({ page }) => {
  await page.goto("/apps/tailwind-components/#/input/TextArea.story");
  await page.getByLabel('Disable input').check();
  await expect(page.getByPlaceholder("This is placeholder text")).toHaveAttribute("disabled");
  const InputTextAreaClass = await page.getByPlaceholder("This is placeholder text").getAttribute("class");
  await expect(InputTextAreaClass).toContain("border-disabled text-disabled bg-disabled");
});

test("InputTextArea: component properly displays placeholder @tw-components @tw-forms @input-textarea", async ({ page }) => {
  await page.goto("/apps/tailwind-components/#/input/TextArea.story");
  
  const newPlaceholder: string = "This is a new placeholder for the textarea component";
  await page.getByLabel("Set placeholder").fill(newPlaceholder);
  await expect(page.getByPlaceholder(newPlaceholder)).toHaveCount(1);
});