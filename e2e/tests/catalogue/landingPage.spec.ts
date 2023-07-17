import { test, expect } from "@playwright/test";

test("View cohorts", async ({ page }) => {
  await page.goto("http://localhost:8080/catalogue-demo/catalogue/#/");
  await page
    .getByRole("link", {
      name: "Cohorts 7 Systematic observations of large groups of individuals over time.",
    })
    .click();
  page.getByText("ALSPAC", { exact: true });
});
