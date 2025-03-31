import { test, expect } from "@playwright/test";

import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}form/AddModal.story?schema=pet+store&table=Pet`);
  await expect(page.getByText("Demo data controls")).toBeVisible();
});

test("should show the add modal", async ({ page }) => {
  await page.getByRole("button", { name: "Add Pet" }).click();
  await expect(page.getByRole("link", { name: "_top" })).toBeVisible();
  await expect(
    page.getByRole("listitem").filter({ hasText: "details" })
  ).toBeVisible();
  await expect(
    page.getByRole("listitem").filter({ hasText: "Heading2" })
  ).toBeVisible();
  await expect(page.getByText("3/3 required fields left")).toBeVisible();
  await expect(page.getByRole("button", { name: "Cancel" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Save draft" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Save", exact: true })
  ).toBeVisible();
  await expect(page.getByText("name Required the name")).toBeVisible();
});
