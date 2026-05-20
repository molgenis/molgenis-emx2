import { expect, test } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";
const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.use({ storageState: "playwright/.auth/user.json" });

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}Form.story?schema=pet+store&table=Pet&rowIndex=1`);
});

test("adding an order via the pet.order refback should update the parent (pet), order list", async ({
  page,
}) => {
  await page.getByRole("button", { name: "Add Order" }).click();
  await page
    .getByRole("textbox", { name: "how many would you like ?" })
    .click();
  await page
    .getByRole("textbox", { name: "how many would you like ?" })
    .fill("3");
  await page.getByRole("textbox", { name: "price" }).click();
  await page.getByRole("textbox", { name: "price" }).fill("3");
  const statusInput = await page.locator(
    '[id="pet store-Order-status-form-field-input"]'
  );
  await statusInput.click();
  await statusInput.fill("e2e");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  // test the order appears in the pet order refback list
  await page
    .locator("ul.border.divide-y > li")
    .nth(2)
    .getByRole("button")
    .filter({ hasText: /^Show details$/ })
    .click();
  await expect(
    page.locator("ul.border.divide-y > li").nth(2).getByText("e2e")
  ).toBeVisible();
});
