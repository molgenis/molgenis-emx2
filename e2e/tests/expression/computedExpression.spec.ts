import { test, expect } from "@playwright/test";
import { login, optionallyRemoveDatabase, createDatabase } from "../utils";

test("computedExpressionCONCAT", async ({ page }) => {
  await login(page);
  await optionallyRemoveDatabase(page, "computedTest");
  await createDatabase(page, "computedTest", "PET_STORE", true);
  await page.goto("/apps/central/#/");

  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("computedTest");
  await page.getByLabel("template").selectOption("PET_STORE");
  await page.getByLabel("Yes").check();
  await page.getByRole("button", { name: "Create database" }).click();
  await page.getByText("Close").click();
  //create computedExpression concat string and REF
  await page.getByRole("link", { name: "computedTest" }).click();
  await page.getByRole("link", { name: "Schema" }).click();
  await expect(page.locator("#molgenis_tables_container")).toContainText(
    "status"
  );
  await page.getByText("status").nth(1).click();
  await page.getByRole("button", { name: " " }).click();
  await page
    .locator(
      "div:nth-child(6) > div:nth-child(3) > .form-group > .input-group > .form-control"
    )
    .click();
  await page
    .locator(
      "div:nth-child(6) > div:nth-child(3) > .form-group > .input-group > .form-control"
    )
    .fill('name+"_"+category?.name');
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  //test if computedExpression works
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
    .fill("youp");
  await page.locator("#Pet-edit-modal-category").getByRole("textbox").click();
  await page
    .getByRole("row", { name: "Select   cat", exact: true })
    .getByRole("button")
    .first()
    .click();
  await page.getByRole("button", { name: "details" }).click();
  await page
    .locator("span")
    .filter({ hasText: "status" })
    .locator("#Pet-edit-modal-status")
    .click();
  await page.locator("input#Pet-edit-modal-weight").click();
  await page.locator("input#Pet-edit-modal-weight").fill("1");
  await page.getByRole("button", { name: "Save Pet" }).click();
  await page.getByRole("button", { name: "filters " }).click();
  await page.getByLabel("name").check();
  await page.getByRole("button", { name: "" }).click();
  await page.locator("#filter-name1").click();
  await page.locator("#filter-name1").fill("youp");
  await expect(page.getByRole("row")).toContainText("youp_cat");
  //remove database
  await page.getByRole("link", { name: "brand-logo" }).click();
  await page
    .getByRole("row", { name: "  computedTest" })
    .getByRole("button")
    .nth(1)
    .click();
  await page.getByRole("button", { name: "Delete database" }).click();
  await page.getByText("Close").click();
});

test("computedExpressionComplex", async ({ page }) => {
  await login(page);
  await optionallyRemoveDatabase(page, "testComputedComplex");
  await createDatabase(page, "testComputedComplex", "PET_STORE", true);
  await page.goto("/apps/central/#/");

  await page.getByRole("link", { name: "testComputedComplex" }).click();
  await page.getByRole("link", { name: "Tag" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Tag-edit-modal-name")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Tag-edit-modal-name")
    .fill("1");
  await page
    .locator("span")
    .filter({ hasText: "labelUser-friendly label for" })
    .locator("#Tag-edit-modal-label")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "labelUser-friendly label for" })
    .locator("#Tag-edit-modal-label")
    .fill("test1");
  await page.getByRole("button", { name: "Save Tag" }).click();
  await page.locator('#Tag-edit-modal-tags').getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Tag-edit-modal-name")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "name (required) name is" })
    .locator("#Tag-edit-modal-name")
    .fill("2");
  await page
    .locator("span")
    .filter({ hasText: "labelUser-friendly label for" })
    .locator("#Tag-edit-modal-label")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "labelUser-friendly label for" })
    .locator("#Tag-edit-modal-label")
    .fill("test2");
  await page.getByRole("button", { name: "Save Tag" }).click();
  await page.getByRole("link", { name: "Schema" }).click();
  await page.getByText("Tables:").click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("Name").click();
  await page.getByLabel("Name").fill("complexVraag");
  await page.getByRole("button", { name: "Apply" }).click();
  await page
    .locator("div")
    .filter({ hasText: /^Columns: None\.$/ })
    .locator("label")
    .click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("columnName").click();
  await page.getByLabel("columnName").fill("total");
  await page.getByLabel("columnType").selectOption("INT");
  await page.getByText("computed", { exact: true }).click();
  await page
    .locator(
      "div:nth-child(6) > div:nth-child(3) > .form-group > .input-group > .form-control"
    )
    .click();
  await page
    .locator(
      "div:nth-child(6) > div:nth-child(3) > .form-group > .input-group > .form-control"
    )
    .fill(
      "parseInt(((typeof vraag1=== 'undefined')?0:vraag1?.name) || 0 )+parseInt(((typeof vraag2=== 'undefined')?0:vraag2?.name) || 0)"
    );
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByText("Columns:").nth(4).click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("columnName").click();
  await page.getByLabel("columnName").fill("vraag2");
  await page.getByLabel("columnType").selectOption("ONTOLOGY");
  await page.getByLabel("refTable").selectOption("Tag");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByText("Columns:").nth(4).click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("columnName").click();
  await page.getByLabel("columnName").fill("vraag1");
  await page.getByLabel("columnType").selectOption("ONTOLOGY");
  await page.getByLabel("refTable").selectOption("Tag");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByText("Columns:").nth(4).click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("columnName").click();
  await page.getByLabel("columnName").fill("id");
  await page.locator("#column_required_radio0").check();
  await page.getByLabel("key").selectOption("1");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  await page.getByRole("link", { name: "Tables" }).click();
  await page.getByRole("link", { name: "complexVraag" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "id (required) id is required" })
    .locator("#ComplexVraag-edit-modal-id")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "id (required) id is required" })
    .locator("#ComplexVraag-edit-modal-id")
    .fill("test1");
  await page
    .locator("#ComplexVraag-edit-modal-vraag1")
    .getByRole("textbox")
    .click();
  await page.getByRole("button", { name: "" }).first().click();
  await page.locator(".overflow-auto").first().click();
  await page.getByText("vraag1test1 test1 test2").click();
  await page
    .locator("#ComplexVraag-edit-modal-vraag2")
    .getByRole("textbox")
    .click();
  await page.getByRole("button", { name: "" }).nth(1).click();
  await page.locator(".overflow-auto").first().click();
  await page.getByRole("button", { name: "Save complexVraag" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page
    .locator("span")
    .filter({ hasText: "id (required) id is required" })
    .locator("#ComplexVraag-edit-modal-id")
    .click();
  await page
    .locator("span")
    .filter({ hasText: "id (required) id is required" })
    .locator("#ComplexVraag-edit-modal-id")
    .fill("test2");
  await page
    .locator("#ComplexVraag-edit-modal-vraag1")
    .getByRole("textbox")
    .click();
  await page.getByRole("button", { name: "" }).nth(1).click();
  await page.locator(".overflow-auto").first().click();
  await page.getByText("vraag1test2 test1 test2").click();
  await page
    .locator("#ComplexVraag-edit-modal-vraag2")
    .getByRole("textbox")
    .click();
  await page.getByRole("button", { name: "test2" }).click();
  await page.getByRole("button", { name: "Save complexVraag" }).click();
  //test if computedExpression works
  await page.getByRole("button", { name: "filters " }).click();
  await page.getByLabel("id").check();
  await page.getByRole("heading", { name: "filters" }).click();
  await page.getByRole("button", { name: "" }).click();
  await page.locator("#filter-id1").click();
  await page.locator("#filter-id1").fill("test1");
  await page.getByRole("heading", { name: "total" }).click();
  await expect(page.getByRole("row")).toContainText("3");
  await page.getByRole("button", { name: "id = test1 " }).click();
  await page.locator("#filter-id1").click();
  await page.locator("#filter-id1").fill("test2");
  await page.getByRole("heading", { name: "total " }).click();
  await expect(page.getByRole("row")).toContainText("4");
});
