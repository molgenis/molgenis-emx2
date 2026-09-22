import { test, expect } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("The tree state of the Diagnosis Available filter", async ({ page }) => {
  await page.goto(getAppRoute());
  await page.locator("#Diagnosisavailable svg").click();

  // Open the first item of the list
  await page
    .getByRole("listitem")
    .filter({ hasText: "▲I Certain infectious and" })
    .locator("span")
    .first()
    .click();
  await page
    .getByRole("listitem")
    .filter({ hasText: /^▲A00-A09 Intestinal infectious diseases$/ })
    .locator("span")
    .first()
    .click();
  await page
    .getByRole("listitem")
    .filter({ hasText: /^▲A00 Cholera$/ })
    .locator("span")
    .first()
    .click();

  // Select A00 Cholera and its children
  await page
    .getByRole("listitem")
    .filter({ hasText: /^▼A00 Cholera$/ })
    .getByRole("checkbox")
    .check();
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: /^▼A00 Cholera$/ })
      .getByRole("checkbox")
  ).toBeChecked();
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: /^▼A00-A09 Intestinal infectious diseases$/ })
      .getByRole("checkbox")
  ).toBeChecked({ indeterminate: true });

  // Deselect its children and check indeterminate state
  await page
    .getByRole("listitem")
    .filter({
      hasText: /^A00\.0 Cholera due to Vibrio cholerae 01, biovar cholerae$/,
    })
    .getByRole("checkbox")
    .uncheck();
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: /^▼A00 Cholera$/ })
      .getByRole("checkbox")
  ).toBeChecked({ indeterminate: true });
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: /^▼A00-A09 Intestinal infectious diseases$/ })
      .getByRole("checkbox")
  ).toBeChecked({ indeterminate: true });
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: "▼I Certain infectious and" })
      .getByRole("checkbox")
  ).toBeChecked({ indeterminate: true });

  await page
    .getByRole("listitem")
    .filter({
      hasText: /^A00\.1 Cholera due to Vibrio cholerae 01, biovar eltor$/,
    })
    .getByRole("checkbox")
    .uncheck();
  await page
    .getByRole("listitem")
    .filter({ hasText: /^A00\.9 Cholera, unspecified$/ })
    .getByRole("checkbox")
    .uncheck();

  // Nothing should be checked
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: /^▼A00 Cholera$/ })
      .getByRole("checkbox")
  ).not.toBeChecked({ indeterminate: false });
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: /^▼A00-A09 Intestinal infectious diseases$/ })
      .getByRole("checkbox")
  ).not.toBeChecked({ indeterminate: false });
  await expect(
    page
      .getByRole("listitem")
      .filter({ hasText: "▼I Certain infectious and" })
      .getByRole("checkbox")
  ).not.toBeChecked({ indeterminate: false });
});
