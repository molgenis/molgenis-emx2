import { test, expect } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("should run playwright", async ({ page }) => {
  await page.goto("https://molgenis.org/");
  expect(await page.title()).toBe("For scientific data");
});

test("directory page should load", async ({ page }) => {
  await page.goto(getAppRoute());
  await expect(page.getByRole("paragraph")).toContainText("© 2024 BBMRI-ERIC");
});