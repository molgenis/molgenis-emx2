import { test, expect } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("directory page should load", async ({ page }) => {
  await page.goto(getAppRoute());
  expect((await page.title()).endsWith("Directory")).toBeTruthy();
});
