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
  await expect(page.getByText("orders Add OrderORDER:")).toBeVisible();
  await page.getByRole("button", { name: "Add Order" }).click();
  await page
    .getByRole("textbox", { name: "how many would you like ?" })
    .click();
  await page
    .getByRole("textbox", { name: "how many would you like ?" })
    .fill("3");
  await page.getByRole("textbox", { name: "price" }).click();
  await page.getByRole("textbox", { name: "price" }).fill("3");
  await page.locator("#fields-container #status-form-field-input").click();
  await page.locator("#fields-container #status-form-field-input").fill("e2e");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  // test the order appears in the pet order refback list
  const orders = page.locator("ul.border.divide-y > li");
  const thirdOrder = orders.nth(2);

  await thirdOrder.getByRole("button").nth(2).click();
  await expect(thirdOrder.getByText("e2e")).toBeVisible();
});
