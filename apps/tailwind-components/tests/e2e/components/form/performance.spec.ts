import { test, expect } from "@playwright/test";
import playwrightConfig from "../../../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(
    `${route}Form.story?schema=patient+registry+demo&table=Subject`
  );
});

test("performance should not degrade when filling form fields in on a large form", async ({
  page,
}) => {
  await page
    .getByRole("textbox", { name: "1.1 SPIDER ID (Individual ID" })
    .click();
  const start = performance.now();
  await page
    .getByRole("textbox", { name: "1.1 SPIDER ID (Individual ID" })
    .fill("i like typing ");
  await page
    .getByRole("textbox", { name: "1.1 SPIDER ID (Individual ID" })
    .press("Tab");
  await page
    .getByRole("textbox", { name: "Alternative ID" })
    .fill("i like to type fast");
  const end = performance.now();
  const timeTaken = end - start;
  console.log(`Time taken to fill the form field: ${timeTaken} milliseconds`);

  // Assert that the values were filled correctly
  await expect(
    page.getByRole("textbox", { name: "1.1 SPIDER ID (Individual ID" })
  ).toHaveValue("i like typing ");
  await expect(
    page.getByRole("textbox", { name: "Alternative ID" })
  ).toHaveValue("i like to type fast");
  expect(timeTaken).toBeLessThan(2000);
});

test("performance of the ontology input should not degrade", async ({
  page,
}) => {
  const start = performance.now();

  await page
    .locator(
      "#hpo-form-field-input-ontology > .flex.items-center.justify-between"
    )
    .click();
  await page.getByPlaceholder("Search in terms").fill("cystic");
  await page
    .getByRole("listitem")
    .filter({ hasText: "Renal tubular atrophyRead more" })
    .locator("rect")
    .click();
  await page.getByPlaceholder("Search in terms").dblclick();
  await page.getByPlaceholder("Search in terms").fill("skin");
  await page
    .getByRole("listitem")
    .filter({ hasText: "EpicanthusRead more" })
    .locator("rect")
    .click();
  await expect(page.getByRole("button", { name: "Epicanthus" })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Renal tubular atrophy" })
  ).toBeVisible();

  const end = performance.now();
  const timeTaken = end - start;
  console.log(
    `Time taken to search and find two ontology values: ${timeTaken} milliseconds`
  );

  expect(timeTaken).toBeLessThan(8000); //seems slow, but in practice it is not slow.
});
