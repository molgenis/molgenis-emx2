import { expect, test } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";
const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test("the inputHyperLink", async ({ page }) => {
  await page.goto(`${route}/input/Hyperlink.story`);
  await expect(page.getByText("Error:")).not.toContainText(
    "Error: Invalid hyperlink"
  );
  await page.fill("#input-hyperlink", "blaat");
  await expect(page.getByText("Error: Invalid hyperlink")).toContainText(
    "Error: Invalid hyperlink"
  );
  await page.getByPlaceholder("https://example.com").clear();
  await page.fill("#input-hyperlink", "https://molgenis.net");
  await expect(page.getByText("Error:")).not.toContainText(
    "Error: Invalid hyperlink"
  );
});
