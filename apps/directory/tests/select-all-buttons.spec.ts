import { test, expect } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("select all collections and services", async ({ page }) => {
  await page.goto(getAppRoute());
  await page.waitForTimeout(5000); // page to become interactive, todo: show loading indicator to user
  await page
    .getByRole("button", { name: "Available to commercial use-filter" })
    .first()
    .click();
  await page.getByRole("button", { name: "Select all collections" }).click();
  await expect(page.getByRole("main")).toContainText("2");
  await page.getByRole("button", { name: "Select all services" }).click();
  await expect(page.getByRole("main")).toContainText("5");
});
