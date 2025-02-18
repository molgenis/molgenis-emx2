import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}Form.story?schema=catalogue-demo&table=Resources`);
  await page.getByText("Jump to", { exact: true }).click({ delay: 300 });
});

test("it should render the form", async ({ page }) => {
  await expect(page.getByRole("main")).toContainText("id");
  await expect(page.getByRole("main")).toContainText("pid");
  await expect(page.getByRole("main")).toContainText("Name");
  await expect(page.getByLabel("id Required", { exact: true })).toBeVisible();
});

test("it should handle input", async ({ page }) => {
  await page
    .getByLabel("name Required", { exact: true })
    .pressSequentially("test");
  await expect(page.getByLabel("name Required", { exact: true })).toHaveValue(
    "test"
  );
});

test("it should show the chapters in the legend", async ({ page }) => {
  await expect(page.locator("a").filter({ hasText: "Overview" })).toBeVisible();
  await expect(
    page.locator("a").filter({ hasText: "design and structure" })
  ).toBeVisible();
});

test("the legend should show number of errors per chapter (if any)", async ({
  page,
}) => {
  await page.getByLabel("name Required", { exact: true }).click();
  // skip a required field
  await page.getByLabel("name Required", { exact: true }).press("Tab");
  await expect(page.locator("span").filter({ hasText: /^1$/ })).toBeVisible();
});

test("clicking on the chapter should scroll to the chapter", async ({
  page,
}) => {
  await page.getByText("population", { exact: true }).first().click();
  await expect(page.getByRole("heading", { name: "population" })).toBeVisible();
});

test("it should update the model value when a field is filled out", async ({
  page,
}) => {
  await page.goto(`${route}Form.story?schema=pet+store&table=Pet`);
  await page.getByText("Jump to", { exact: true }).click({ delay: 300 });
  await page.getByLabel("name Required", { exact: true }).click();
  await page.getByLabel("name Required", { exact: true }).fill("test");
  await expect(page.getByLabel("name Required", { exact: true })).toHaveValue(
    "test"
  );
});
