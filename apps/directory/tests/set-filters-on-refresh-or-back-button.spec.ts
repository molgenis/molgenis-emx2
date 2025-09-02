import { expect, test } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("Refreshing and going back should correctly set the filters in the url", async ({
  page,
}) => {
  await page.goto(getAppRoute());

  //filter
  await expect(page.getByText("Organisations: 3")).toBeVisible();
  await page.getByText("Material type").click();
  await page.getByRole("checkbox", { name: "DNA", exact: true }).check();
  await expect(page.getByText("Organisations: 1")).toBeVisible();
  await page.reload();
  await expect(page.getByText("Organisations: 1")).toBeVisible();

  //cart
  await page.getByRole("button", { name: "Select all collections" }).click();
  await expect(page.getByText("Request1")).toBeVisible();
  await page.reload();
  await expect(page.getByText("Organisations: 1")).toBeVisible();
  await expect(page.getByText("Request1")).toBeVisible();

  //back
  await page.getByRole("link", { name: "Biobank1", exact: true }).click();
  await expect(page.getByRole("main")).toContainText(
    "bbmri-eric:ID:DE_biobank1"
  );
  await page.goBack();
  await expect(page.getByText("Request1")).toBeVisible();

  await expect(page.getByText("Organisations: 1")).toBeVisible();

  //reset filters
  await page.getByRole("button", { name: "Clear all filters" }).click();
  await expect(page.getByText("Organisations: 3")).toBeVisible();
  // Togglefilter
  await page
    .getByRole("button", { name: "Available to commercial use" })
    .click();
  await expect(page.getByText("Organisations: 2")).toBeVisible();
  await page.reload();
  await expect(page.getByText("Organisations: 2")).toBeVisible();
});
