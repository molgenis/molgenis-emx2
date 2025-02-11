import { test, expect } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("test", async ({ page }) => {
  await page.goto(getAppRoute());
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByRole("textbox", { name: "Username" }).click();
  await page.getByRole("textbox", { name: "Username" }).fill("admin");
  await page.getByRole("textbox", { name: "Username" }).press("Tab");
  await page.getByRole("textbox", { name: "Password" }).fill("admin");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Sign in" })
    .click();
  await page.getByRole("main").getByRole("link", { name: "Settings" }).click();
  await page.getByRole("button", { name: "JSON" }).click();

  await page
    .getByRole("code")
    .locator("div")
    .filter({ hasText: '"language": "en"' })
    .nth(4)
    .click();
  await page.keyboard.type('footer: "Very pretty footer",');
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByRole("contentinfo")).toContainText(
    "Very pretty footer"
  );
});
