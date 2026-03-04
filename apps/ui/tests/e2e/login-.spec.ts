import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

test("the session should be updated after login", async ({ page }) => {
  await page.goto(route);
  await expect(page.getByRole("banner")).toContainText("Signin");
  await page.getByRole("button", { name: "Signin" }).click();
  await page.getByRole("textbox", { name: "Username" }).click();
  await page.getByRole("textbox", { name: "Username" }).fill("admin");
  await page.getByRole("textbox", { name: "Username" }).press("Tab");
  await page.getByRole("textbox", { name: "Password" }).fill("admin");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("banner")).toContainText("Account");
  await page.getByRole("button", { name: "Account" }).click();
  await page.getByRole("button", { name: "Sign out" }).click();
  await expect(page.getByRole("banner")).toContainText("Signin");
});
