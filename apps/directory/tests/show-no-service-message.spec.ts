import { test, expect } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("show message if biobank has no services", async ({ page }) => {
  await page.goto(getAppRoute());
  await page.getByRole("button", { name: "Services" }).nth(2).click();
  await expect(page.getByRole("main")).toContainText(
    "This biobank has no services yet."
  );
});
