import type { APIRequestContext } from "@playwright/test";
import { request as apiRequest, expect, test } from "@playwright/test";
import playwrightConfig from "../../playwright.config";
import {
  addPasswordToUser,
  addRlsToTables,
  becomeAdmin,
  dropAnonymousFromPetStore,
  findAndDeleteRow,
  insertRow,
  removeRlsFromTables,
  restoreAnonymousToPetStore,
  signin,
  signout,
} from "./testUtils/testUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

let api: APIRequestContext;
const USERNAME = "dragonkeeper";
const PASSWORD = "dragonkeeper";
const PET_STORE_PATH = "pet%20store/Pet";

test.describe.configure({ mode: "serial" });

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await becomeAdmin(api);
  await dropAnonymousFromPetStore(api);
  await addPasswordToUser(api, USERNAME, PASSWORD);
});

test.afterAll(async () => {
  await restoreAnonymousToPetStore(api);
  await api.dispose();
});

test.describe("when the dragonkeeper has permissions on the pet table only", () => {
  test("The dragonkeeper has the correct permissions", async ({ page }) => {
    await page.goto(route + PET_STORE_PATH);
    await expect(
      page.getByText("The requested page could not be found.")
    ).toBeVisible();
    await page.getByRole("button", { name: "Home" }).click();
    await expect(page).toHaveURL(route);

    await signin(page, USERNAME, PASSWORD);

    // check that other tables are not clickable
    await page.goto(route);
    await page.getByText("pet store", { exact: true }).click();
    await expect(page.getByText("Category")).toBeVisible();
    await expect(page.getByText("Order")).toBeVisible();
    await expect(page.getByText("User")).toBeVisible();

    await page.getByText("Pet", { exact: true }).click();
    await expect(page.getByText("No records found")).toBeVisible();

    await signout(page);
    await expect(page.getByLabel("error")).toBeVisible();

    await signin(page, "admin", "admin");
    await page.getByRole("searchbox", { name: "Search Pet" }).fill("pooky");
    await expect(
      page
        .locator("div")
        .filter({ hasText: /^pooky$/ })
        .first()
    ).toBeVisible();
  });
});

test.describe("when the dragonkeeper has also permissions on the order table", () => {
  test.beforeAll(async () => {
    await addRlsToTables(api);
  });

  test.afterAll(async () => {
    await removeRlsFromTables(api);
  });

  test("the dragonkeeper can now see the order table", async ({ page }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText("pet store", { exact: true }).click();
    await page.getByText("Order", { exact: true }).click();
    await expect(page.getByText("No records found")).toBeVisible();
  });

  test("the dragonkeeper can now see smaug in the pet table", async ({
    page,
  }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText("pet store", { exact: true }).click();
    await page.getByText("Pet", { exact: true }).click();
    await expect(
      page
        .locator("div")
        .filter({ hasText: /^smaug$/ })
        .first()
    ).toBeVisible();
  });

  test("the dragonkeeper can now add pets to the pet table", async ({
    page,
  }) => {
    await page.goto(route);
    await signin(page, USERNAME, PASSWORD);
    await page.getByText("pet store", { exact: true }).click();
    await page.getByText("Pet", { exact: true }).click();
    await page.getByRole("button", { name: "Add" }).click();
    await expect(page.getByLabel("Name")).toBeVisible();
  });
});

test.describe("when selecting a permission for a row", () => {
  test("as admin, for a new row", async ({ page }) => {
    await page.goto(route);
    await signin(page, "admin", "admin");
    await page.goto(route + PET_STORE_PATH);

    await page.getByRole("button", { name: "Add Pet" }).click();
    await page.getByLabel("Permission level:").selectOption("DragonKeeper");
    await page
      .getByRole("textbox", { name: "name Required" })
      .fill("testDragon");
    await page
      .locator('[id="pet store-Pet-category-form-field-input-radio-group"]')
      .getByText("dragon", { exact: true })
      .click();
    await page.getByRole("textbox", { name: "weight Required" }).fill("50000");

    await insertRow(page, "Pet");

    await findAndDeleteRow(page, "Pet", "testDragon");
  });

  test("as manager,when editing a row", async ({ page }) => {
    await page.goto(route);
    await signin(page, "shopmanager", "shopmanager");
    await page.goto(route + PET_STORE_PATH);

    await expect(
      page.locator("tr:nth-child(10) > td:nth-child(2) > .flex")
    ).toHaveText("DragonKeeper", { exact: true });

    await page.getByRole("cell", { name: "smaug" }).hover();
    await page.getByRole("button", { name: 'edit {"name":"smaug"}' }).click();
    await page.getByLabel("Permission level:").selectOption("Global");
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await page.getByRole("button", { name: "Cancel" }).click();

    await expect(
      page.locator("tr:nth-child(10) > td:nth-child(2) > .flex")
    ).toHaveText("", { exact: true });

    await page.getByRole("cell", { name: "smaug" }).hover();
    await page.getByRole("button", { name: 'edit {"name":"smaug"}' }).click();
    await page.getByLabel("Permission level:").selectOption("DragonKeeper");
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await page.getByRole("button", { name: "Cancel" }).click();

    await expect(
      page.locator("tr:nth-child(10) > td:nth-child(2) > .flex")
    ).toHaveText("DragonKeeper", { exact: true });
  });
});
