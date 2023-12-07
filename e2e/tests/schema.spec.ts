import { test, expect } from "@playwright/test";

test("Create a simple table with the schema editor", async ({ page }) => {
  await page.goto("http://localhost:8080/apps/central/");
  await page.goto("http://localhost:8080/apps/central/#/");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByPlaceholder("Enter username").click();
  await page.getByPlaceholder("Enter username").fill("admin");
  await page.getByPlaceholder("Enter username").press("Tab");
  await page.getByPlaceholder("Password").fill("admin");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Sign in" })
    .click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("schematest");
  await page.getByRole("button", { name: "Create database" }).click();
  await page.getByText("Close").click();
  await page.getByRole("link", { name: "schematest" }).click();
  await page.getByRole("link", { name: "Schema", exact: true }).click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("Name").click();
  await page.getByLabel("Name").fill("TestTable");
  await page
    .locator("div")
    .filter({ hasText: /^label$/ })
    .getByRole("textbox")
    .click();
  await page
    .locator("div")
    .filter({ hasText: /^label$/ })
    .getByRole("textbox")
    .fill("TestTable");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Cancel" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  await page.getByLabel("columnName").click();
  await page.getByLabel("columnName").fill("test");
  await page.getByLabel("columnName").dblclick();
  await page.getByLabel("columnName").dblclick();
  await page.locator("#column_required0").check();
  await page.getByLabel("key").selectOption("1");
  await page.locator("textarea").first().click();
  await page.locator("textarea").first().fill("test");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByLabel("columnName").click();
  await page.getByLabel("columnName").fill("boolean");
  await page.getByLabel("columnType").selectOption("BOOL");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByLabel("columnName").click();
  await page.getByLabel("columnName").fill("defaultvalue");
  await page.getByLabel("defaultValue").click();
  await page.getByLabel("defaultValue").fill("defaultvalue");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  await page.getByRole("link", { name: "Tables" }).click();
  await page.getByRole("link", { name: "TestTable" }).click();
  await page.getByRole("link", { name: "Schema", exact: true }).click();
  await page.getByRole("link", { name: "brand-logo" }).click();
  await page
    .getByRole("row", { name: "schematest" })
    .getByRole("button")
    .nth(1)
    .click();
  await page.getByRole("button", { name: "Delete database" }).click();
  await page.getByText("Close").click();
});

test("Create a ontology with the schema editor", async ({ page }) => {
  await page.goto("http://localhost:8080/apps/central/");
  await page.goto("http://localhost:8080/apps/central/#/");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.getByPlaceholder("Enter username").click();
  await page.getByPlaceholder("Enter username").fill("admin");
  await page.getByPlaceholder("Enter username").press("Tab");
  await page.getByPlaceholder("Password").fill("admin");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Sign in" })
    .click();
  await page.getByRole("button", { name: "" }).click();
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("schemaontology");
  await page.getByLabel("name").click();
  await page.getByRole("button", { name: "Create database" }).click();
  await page.getByText("Close").click();
  await page.getByRole("link", { name: "schemaontology" }).click();
  await page.getByRole("link", { name: "Schema", exact: true }).click();
  await page.getByLabel("Name").click();
  await page.getByLabel("Name").fill("testontology");
  await page.getByRole("button", { name: "Apply" }).click();
  await page.getByRole("button", { name: "Save" }).click();
  await page.getByRole("link", { name: "testontology" }).click();
  await page.getByRole("link", { name: "Tables" }).click();
  await page.getByRole("link", { name: "testontology" }).click();
  await page.getByRole("link", { name: "< schemaontology" }).click();
  await page.getByRole("link", { name: "brand-logo" }).click();
  await page
    .getByRole("row", { name: "schemaontology" })
    .getByRole("button")
    .nth(1)
    .click();
  await page.getByRole("button", { name: "Delete database" }).click();
});
