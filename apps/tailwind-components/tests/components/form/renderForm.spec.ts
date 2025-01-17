import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test("it should render the form", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.goto(
    "https://emx2.dev.molgenis.org/apps/tailwind-components/#/Form.story"
  );
  await expect(page.getByRole("main")).toContainText("bool");
  await expect(page.getByRole("main")).toContainText(
    "place holder for field type BOOL"
  );
  await expect(page.getByRole("main")).toContainText("date");
  await expect(page.getByRole("main")).toContainText("name");
  await expect(page.getByRole("main")).toContainText("Required");
  await expect(page.getByRole("main")).toContainText("the name");
  await expect(page.getByLabel("name")).toBeVisible();
});

test("it should handle input", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("test");
  await expect(page.getByLabel("name")).toHaveValue("test");
});
