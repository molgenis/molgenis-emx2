import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}ThemeSwitch.story`);
  await page
    .getByText("ThemeSwitch", { exact: true })
    .first()
    .click({ delay: 300 });
});

test("the theme switch should toggle between light and dark", async ({
  page,
}) => {
  const toggle = await page.getByRole("button", { name: "Toggle Theme" });
  await expect(toggle).toBeVisible();
  await expect(page.getByTestId("cookie-theme")).toContainText(
    "cookie theme: "
  );
  await page.getByRole("button", { name: "Toggle theme" }).first().click();
  await expect(page.getByTestId("cookie-theme")).toContainText(
    "cookie theme: dark"
  );
  await page.getByRole("button", { name: "Toggle theme" }).click();
  await expect(page.getByTestId("cookie-theme")).toContainText(
    "cookie theme: light"
  );
});
