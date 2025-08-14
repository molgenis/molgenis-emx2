import { expect, test } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("Refreshing and going back should correctly set the filters in the url", async ({
  page,
}) => {
  await page.goto(getAppRoute());
  await expect(page.getByRole("main")).toContainText("Organisations: 3");
  await page.getByText("Material type").click();
  await page.getByRole("checkbox", { name: "DNA", exact: true }).check();
  await expect(page.getByRole("main")).toContainText("Organisations: 1");
  await page.reload();
  await expect(page.getByRole("main")).toContainText("Organisations: 1");
  await expect(page.getByRole("main")).toContainText("Request1");
  await page.reload();
  await expect(page.getByRole("main")).toContainText("Organisations: 1");
  await expect(page.getByRole("main")).toContainText("Request1");
  await page.getByRole("link", { name: "Biobank1", exact: true }).click();
  await expect(page.getByRole("main")).toContainText(
    "bbmri-eric:ID:DE_biobank1"
  );
  await page.goBack();
  await expect(page.getByRole("main")).toContainText("Request1");
  await expect(page.getByRole("main")).toContainText("Organisations: 1");
});
