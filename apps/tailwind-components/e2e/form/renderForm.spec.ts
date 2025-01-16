import { expect, test } from "@nuxt/test-utils/playwright";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test("it should render the form", async ({ page }) => {
  await page.goto(`${route}Form.story`);
  await expect(page.getByText("name", { exact: true })).toBeVisible();
  await expect(
    page
      .locator("div")
      .filter({ hasText: /^nameRequired$/ })
      .locator("span")
  ).toBeVisible();
  await expect(page.getByText("the name")).toBeVisible();
});

test("it should update the model value when a field is filled out", async ({
  page,
  goto,
}) => {
  await goto(`${route}Form.story`, { waitUntil: "hydration" });
  await page.getByLabel("name").click();
  await page.getByLabel("name").fill("test");
  await expect(page.getByRole("term")).toContainText("name:");
  await expect(page.getByRole("definition")).toContainText("test");
});
