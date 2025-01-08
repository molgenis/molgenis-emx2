import { test, expect } from "@playwright/test";
import { login, optionallyRemoveDatabase, createDatabase } from "../utils";

test("visibleExpressionDECIMAL", async ({ page }) => {
  await login(page);
  await optionallyRemoveDatabase(page, "testVisibleINT");
  await createDatabase(page, "testVisibleINT", "PET_STORE", true);
  await page.goto("/apps/central/#/");

  await page.getByRole("link", { name: "testVisibleINT" }).click();
  await page.getByRole("link", { name: "Schema" }).click();
  await page.getByText("complete").click();
  await page.getByRole("button", { name: " " }).click();
  await page
    .locator("div:nth-child(2) > .form-group > .input-group > .form-control")
    .click();
  await page
    .locator("div:nth-child(2) > .form-group > .input-group > .form-control")
    .fill("price >7");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  //test if visibleExpression DECIMAL/INT works
  await page.getByRole("link", { name: "Tables" }).click();
  await page.getByRole("link", { name: "Order" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page.locator("input#Pet-edit-modal-weight").click();
  await page.locator("input#Pet-edit-modal-weight").fill("8");
  await page.locator("input#Pet-edit-modal-weight").check();
  await expect(
    page.locator("#Order-edit-modal-complete").getByText("complete")
  ).toBeVisible();
  await expect(page.locator("#Order-edit-modal-complete")).toContainText(
    "Yes"
  );
  await page.getByRole("button", { name: "Save Order" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page.locator("input#Pet-edit-modal-weight").click();
  await page.locator("input#Pet-edit-modal-weight").fill("7");
  await expect(
    page.locator("#Order-edit-modal-complete").getByText("complete")
  ).toBeHidden();
  await page.getByRole("button", { name: "Save Order" }).click();
});

test("visibleExpressionREF", async ({ page }) => {
  await login(page);
  await optionallyRemoveDatabase(page, "testVisibleREF");
  await createDatabase(page, "testVisibleREF", "PET_STORE", true);
  await page.goto("/apps/central/#/");

  await page.getByRole("link", { name: "testVisibleREF" }).click();
  await page.getByRole("link", { name: "Schema" }).click();
  await page.getByText("status").first().click();
  await page.getByRole("button", { name: " " }).click();
  await page
    .locator("div:nth-child(2) > .form-group > .input-group > .form-control")
    .click();
  await page
    .locator("div:nth-child(2) > .form-group > .input-group > .form-control")
    .fill(' pet?.name==="spike"');
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  //test if visibleExpression REF works
  await page.getByRole("link", { name: "Tables" }).click();
  await page.getByRole("link", { name: "Order" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page.locator("#Order-edit-modal-pet").getByRole("textbox").click();
  await page
    .getByRole("row", { name: "Select   spike dog sold red" })
    .getByRole("button")
    .first()
    .click();
  await expect(page.getByRole("dialog").getByText("status")).toBeVisible();
  await page.getByRole("button", { name: "Save Order" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page.locator("#Order-edit-modal-pet").getByRole("textbox").click();
  await page
    .getByRole("row", { name: "Select   pooky cat" })
    .getByRole("button")
    .first()
    .click();
  await expect(page.getByRole("dialog").getByText("status")).toBeHidden();
  await page.getByRole("button", { name: "Save Order" }).click();
});

test("visibleExpressionONTOLOGY_ARRAY", async ({ page }) => {
  await login(page);
  await optionallyRemoveDatabase(page, "testVisibleONTOLOGY_ARRAY");
  await createDatabase(page, "testVisibleONTOLOGY_ARRAY", "PET_STORE", true);
  await page.goto("/apps/central/#/");

  //create visibleExpression
  await page.getByRole("link", { name: "testVisibleONTOLOGY_ARRAY" }).click();
  await page.getByRole("link", { name: "Schema" }).click();
  await page.getByRole("heading", { name: "Table: Pet" }).click();
  await page.getByText("status").nth(1).click();
  await page.getByRole("button", { name: " " }).click();
  await page
    .locator("div:nth-child(2) > .form-group > .input-group > .form-control")
    .click();
  await page
    .locator("div:nth-child(2) > .form-group > .input-group > .form-control")
    .fill('tags?.some(tags=>tags.name === "colors")');
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  //test if visibleExpression ONTOLOGY_ARRAY works
  await page.getByRole("link", { name: "Tables" }).click();
  await page.getByRole("link", { name: "Pet" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Pet-edit-modal-name")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Pet-edit-modal-name")
    .fill("test");
  await page.locator("#Pet-edit-modal-category").getByRole("textbox").click();
  await page
    .getByRole("row", { name: "Select   cat", exact: true })
    .getByRole("button")
    .first()
    .click();
  await page.getByRole("button", { name: "details" }).click();
  await page.locator("#Pet-edit-modal-tags div").nth(2).click();
  await page.getByRole("button", { name: "" }).first().click();
  await expect(page.getByRole("dialog").getByText("status")).toBeVisible();
  await page
    .locator("span")
    .filter({ hasText: "status" })
    .locator("#Pet-edit-modal-status")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "status" })
    .locator("#Pet-edit-modal-status")
    .fill("2");
  await page.locator("input#Pet-edit-modal-weight").click();
  await page.locator("input#Pet-edit-modal-weight").fill("2");
  await page.getByRole("button", { name: "Save Pet" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Pet-edit-modal-name")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Pet-edit-modal-name")
    .fill("test2");
  await page.locator("#Pet-edit-modal-category").getByRole("textbox").click();
  await page
    .getByRole("row", { name: "Select   cat", exact: true })
    .getByRole("button")
    .first()
    .click();
  await page.getByRole("button", { name: "details" }).click();
  await page.locator("#Pet-edit-modal-tags").getByRole("textbox").click();
  await page.getByRole("button", { name: "species (3)" }).click();
  await page.getByRole("button", { name: "" }).nth(1).click();
  await expect(page.getByRole("dialog").getByText("status")).toBeHidden();
  await page.locator(".overflow-auto").first().click();
  await page.getByText("Chaptersnamedetails").click();
  await page.locator("input#Pet-edit-modal-weight").click();
  await page.locator("input#Pet-edit-modal-weight").fill("2");
  await page.locator("input#Pet-edit-modal-weight").click();
  await page.getByRole("button", { name: "Save Pet" }).click();
});
