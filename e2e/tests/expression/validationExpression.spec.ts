import { test, expect } from "@playwright/test";

test("test", async ({ page }) => {
  await page.goto("/apps/central/#/");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByPlaceholder("Enter username").click();
  await page.getByPlaceholder("Enter username").fill("admin");
  await page.getByPlaceholder("Password").click();
  await page.getByPlaceholder("Password").fill("admin");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Sign in" })
    .click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("testValidationExpression");
  await page.getByLabel("template").selectOption("PET_STORE");
  await page.getByLabel("true").check();
  await page.getByRole("button", { name: "Create database" }).click();
  await page.getByText("Close").click();
  await page.getByRole("link", { name: "brand-logo" }).click();
  await page.getByRole("link", { name: "testValidationExpression" }).click();
  await page.getByRole("link", { name: "Schema" }).click();
  await page.getByText("quantity").click();
  await page.getByRole("button", { name: " " }).click();
  await page
    .locator(
      "div:nth-child(6) > div > .form-group > .input-group > .form-control"
    )
    .first()
    .click();
  await page
    .locator(
      "div:nth-child(6) > div > .form-group > .input-group > .form-control"
    )
    .first()
    .press("Meta+a");
  await page
    .locator(
      "div:nth-child(6) > div > .form-group > .input-group > .form-control"
    )
    .first()
    .fill("if(quantity < 2) 'quantity should be >= 2'");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  await page.getByRole("link", { name: "Tables" }).click();
  await page.getByRole("link", { name: "Order" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "quantityInvalid value: must" })
    .locator("#Order-edit-modal-quantity")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "quantityInvalid value: must" })
    .locator("#Order-edit-modal-quantity")
    .fill("0");
  await expect(page.locator("small")).toContainText(
    "Applying validation rule returned error: quantity should be >= 2"
  );
  await page
    .locator("span")
    .filter({ hasText: "quantityApplying validation" })
    .locator("#Order-edit-modal-quantity")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "quantityApplying validation" })
    .locator("#Order-edit-modal-quantity")
    .fill("1");
  await page
    .locator("span")
    .filter({ hasText: "quantityApplying validation" })
    .locator("#Order-edit-modal-quantity")
    .press("Enter");
  await expect(page.locator("small")).toContainText(
    "Applying validation rule returned error: quantity should be >= 2"
  );
  await page
    .locator("span")
    .filter({ hasText: "quantityApplying validation" })
    .locator("#Order-edit-modal-quantity")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "quantityApplying validation" })
    .locator("#Order-edit-modal-quantity")
    .fill("2");
  await expect(page.getByText("Applying validation rule")).toBeHidden();
  await page.getByLabel("Close").click();
  await page.getByRole("link", { name: "brand-logo" }).click();
  await page
    .getByRole("row", { name: "  testValidationExpression" })
    .getByRole("button")
    .nth(1)
    .click();
  await page.getByRole("button", { name: "Delete database" }).click();
  await page.getByText("Close").click();
});
