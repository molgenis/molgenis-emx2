import { test, expect, type Page } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

// sorts after the pet store demo pets, so with a page size of 10 it ends up on page 2
const PET_NAME = "zz-e2e-show-saved-row";

async function signIn(page: Page) {
  if (await page.getByRole("button", { name: "Signin" })) {
    await page.getByRole("button", { name: "Signin" }).click();
    await page.getByRole("textbox", { name: "Username" }).fill("admin");
    await page.getByRole("textbox", { name: "Password" }).fill("admin");
    await page.getByRole("button", { name: "Sign in" }).click();
  }
  await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
}

// removes the test pet if it exists, e.g. left over from an earlier failed run
async function deleteTestPet(page: Page) {
  await page.goto(`${route}pet%20store/Pet?mg_search=${PET_NAME}`);
  await expect(
    page.getByText(
      /Showing \d+ to \d+ of \d+ items|No data matched the filters/
    )
  ).toBeVisible();
  const row = page.getByRole("row").filter({ hasText: PET_NAME });
  if (await row.count()) {
    await row.hover();
    await page
      .getByRole("button", { name: `delete {"name":"${PET_NAME}"}` })
      .click();
    await page.getByRole("button", { name: "Delete", exact: true }).click();
    await expect(row).toHaveCount(0);
  }
}

test.afterEach(async ({ page }) => {
  await deleteTestPet(page);
});

test("shows and highlights a row after it is added or updated", async ({
  page,
}) => {
  await page.goto(`${route}pet%20store/Pet`);
  await signIn(page);
  await deleteTestPet(page);
  // signing in reloads the page without query params, so set the page size after
  await page.goto(`${route}pet%20store/Pet?pagesize=10`);
  await expect(page.getByRole("button", { name: "Add Pet" })).toBeVisible();

  // add the row from page 1
  await page.getByRole("button", { name: "Add Pet" }).click();
  await page.getByRole("textbox", { name: "name Required" }).fill(PET_NAME);
  await page
    .locator('[id="pet store-Pet-category-form-field-input-radio-group"]')
    .getByText("cat", { exact: true })
    .click();
  // work around for scroll to next required field
  await page.getByRole("link", { name: "Heading2" }).click();
  await page.getByRole("textbox", { name: "weight Required" }).fill("23");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByText("inserted Pet")).toBeVisible();
  await page.getByRole("button", { name: "Close modal", exact: true }).click();

  // the table goes to the page with the new row and keeps the other url params
  await expect(page).toHaveURL(/[?&]page=2(&|$)/);
  await expect(page).toHaveURL(/[?&]pagesize=10(&|$)/);
  const highlightedRow = page.locator("tr[data-highlighted]");
  await expect(highlightedRow).toContainText(PET_NAME);

  // after an update the row stays on its page and is highlighted again
  await expect(highlightedRow).toHaveCount(0, { timeout: 10000 });
  await page.getByRole("row").filter({ hasText: PET_NAME }).hover();
  await page
    .getByRole("button", { name: `edit {"name":"${PET_NAME}"}` })
    .click();
  await page.getByRole("textbox", { name: "weight Required" }).fill("24");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByText("saved Pet")).toBeVisible();
  await page.getByRole("button", { name: "Close modal", exact: true }).click();

  await expect(page).toHaveURL(/[?&]page=2(&|$)/);
  await expect(highlightedRow).toContainText(PET_NAME);
});
