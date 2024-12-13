import { test, expect } from "@playwright/test";
import { signin } from "./signin";
import { getAppRoute } from "./getAppRoute";

test("landing page cards set filter", async ({ page }) => {
  await page.goto(getAppRoute());
  await signin(page);
  await expect(
    page.getByRole("main").getByRole("link", { name: "Settings" })
  ).toHaveText("Settings");
  await page.getByRole("main").getByRole("link", { name: "Settings" }).click();

  await page.getByRole("button", { name: "Landingpage" }).click();
  await expect(page.getByLabel("Landingpage enabled")).toBeVisible();

  await page.getByLabel("Landingpage enabled").evaluate((e) => {
    const checkBox = e as HTMLInputElement;
    if (!checkBox.checked) {
      checkBox.click();
    }
  });

  await expect(
    page.getByRole("link", { name: "Cardiovascular Deseases" })
  ).toBeVisible();
  await page.getByRole("link", { name: "Rare Diseases" }).click();
  await expect(page.getByText("Because you searched for:")).toBeVisible();

  await expect(
    page.getByRole("article").getByText("Rare Diseases")
  ).toBeVisible();

  // disable landing page
  await expect(
    page.getByRole("main").getByRole("link", { name: "Settings" })
  ).toHaveText("Settings");
  await page.getByRole("main").getByRole("link", { name: "Settings" }).click();

  await page.getByRole("button", { name: "Landingpage" }).click();
  await expect(page.getByLabel("Landingpage enabled")).toBeVisible();

  await page.getByLabel("Landingpage enabled").evaluate((e) => {
    const checkBox = e as HTMLInputElement;
    if (checkBox.checked) {
      checkBox.click();
    }
  });
});
