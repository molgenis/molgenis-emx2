import { expect, test } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("Refreshing and going back should correctly set the filters in the url", async ({
  page,
}) => {
  await page.goto(getAppRoute());

  //filter
  await expect(page.getByRole("main")).toContainText("Organisations: 3");
  await page.getByText("Material type").click();
  await page.getByRole("checkbox", { name: "DNA", exact: true }).check();
  await expect(page.getByRole("main")).toContainText("Organisations: 1");
  await page.reload();
  await expect(page.getByRole("main")).toContainText("Organisations: 1");

  //cart
  await page.getByRole("button", { name: "Select all collections" }).click();
  await expect(page.getByRole("main")).toContainText("Request1");
  await page.reload();
  await expect(page.getByRole("main")).toContainText("Organisations: 1");
  await expect(page.getByRole("main")).toContainText("Request1");

  //back
  await page.getByRole("link", { name: "Biobank1", exact: true }).click();
  await expect(page.getByRole("main")).toContainText(
    "bbmri-eric:ID:DE_biobank1"
  );
  await page.goBack();
  await expect(page.getByRole("main")).toContainText("Request1");
  await expect(page.getByRole("main")).toContainText("Organisations: 1");

  //reset filters
  await page.getByRole("button", { name: "Clear all filters" }).click();
  await expect(page.getByRole("main")).toContainText("Organisations: 3");

  // Togglefilter
  await page
    .getByRole("button", { name: "Available to commercial use" })
    .click();
  await expect(page.getByRole("main")).toContainText("Organisations: 2");
  await page.reload();
  await expect(page.getByRole("main")).toContainText("Organisations: 2");
});
