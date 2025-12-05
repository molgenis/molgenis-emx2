import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(
    `${route}form/EditModal.story?schema=pet+store&table=Pet&rowIndex=3`,
    { waitUntil: "networkidle" }
  );
  await expect(page.getByText("Edit Pet")).toBeVisible();
});

test("should show the edit modal", async ({ page }) => {
  await expect(page.getByText("Edit Pet")).toBeVisible();
  await page.getByRole("button", { name: "Edit Pet" }).click();
  await expect(page.getByRole("link", { name: "_top" })).toBeVisible();
  await expect(
    page.getByRole("link").filter({ hasText: "details" })
  ).toBeVisible();
  await expect(
    page.getByRole("link").filter({ hasText: "Heading2" })
  ).toBeVisible();
  await expect(page.getByText("All required fields are filled")).toBeVisible();
  await expect(page.getByRole("button", { name: "Cancel" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Save as draft" })
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Save", exact: true })
  ).toBeVisible();
  await expect(page.getByText("name Required the name")).toBeVisible();
});

test("should validate form before updating", async ({ page }) => {
  await expect(page.getByText("Edit Pet")).toBeVisible();
  await page.getByRole("button", { name: "Edit Pet" }).click();

  await page.getByRole("textbox", { name: "weight Required" }).click();
  await page.getByRole("textbox", { name: "weight Required" }).fill("");

  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    page.getByText("1 field requires attention before you can save this cohort")
  ).toBeVisible();
  await page.getByRole("button", { name: "go to next error" }).click();
  await expect(page.getByText("weight is required")).toBeVisible();
});
