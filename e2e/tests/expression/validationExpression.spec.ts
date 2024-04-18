import { test, expect } from "@playwright/test";
import { login, optionallyRemoveDatabase, createDatabase } from "../utils";

test("validation expression", async ({ page }) => {
  await login(page);
  await optionallyRemoveDatabase(page, "testValidationExpression");
  await createDatabase(page, "testValidationExpression", "PET_STORE", true);
  await page.goto("/apps/central/#/");

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
});
