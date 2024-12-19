import test, { expect } from "@playwright/test";
import { getAppRoute } from "./getAppRoute";

test("should show an error when navigating to an unknown biobank", async ({
  page,
}) => {
  await page.goto(getAppRoute() + "#/biobank/nonexistentBiobank");
  await expect(page.getByText("Biobank not found")).toContainText(
    "Biobank not found"
  );
});

test("should show an error when navigating to an unknown collection", async ({
  page,
}) => {
  await page.goto(getAppRoute() + "#/collection/nonexistentCollection");
  await expect(page.getByText("Collection not found")).toContainText(
    "Collection not found"
  );
});

test("should show an error when navigating to an unknown study", async ({
  page,
}) => {
  await page.goto(getAppRoute() + "#/study/nonexistentStudy");
  await expect(page.getByText("Study not found")).toContainText(
    "Study not found"
  );
});

test("should show an error when navigating to an unknown service", async ({
  page,
}) => {
  await page.goto(getAppRoute() + "#/service/nonexistentService");
  await expect(page.getByText("Service not found")).toContainText(
    "Service not found"
  );
});
