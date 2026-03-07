import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/tailwind-components/#/";

test.beforeEach(async ({ page }) => {
  await page.goto(`${route}ButtonBar.story`);
});

test("only the left edge of the first button and the right edge of the last button should have rounded input style", async ({
  page,
}) => {
  await expect(page.getByRole("button", { name: "one" }).nth(1)).toBeVisible();

  const first = await page.getByRole("button", { name: "one" }).nth(1);
  await expect(first).toHaveCSS("border-top-left-radius", "3px");
  await expect(first).toHaveCSS("border-bottom-left-radius", "3px");
  await expect(first).toHaveCSS("border-top-right-radius", "0px");
  await expect(first).toHaveCSS("border-bottom-right-radius", "0px");

  const last = await page.getByRole("button", { name: "four" });
  await expect(last).toHaveCSS("border-top-left-radius", "0px");
  await expect(last).toHaveCSS("border-bottom-left-radius", "0px");
  await expect(last).toHaveCSS("border-top-right-radius", "3px");
  await expect(last).toHaveCSS("border-bottom-right-radius", "3px");

  const middle = await page.getByRole("button", { name: "three" });
  await expect(middle).toHaveCSS("border-top-left-radius", "0px");
  await expect(middle).toHaveCSS("border-bottom-left-radius", "0px");
  await expect(middle).toHaveCSS("border-top-right-radius", "0px");
  await expect(middle).toHaveCSS("border-bottom-right-radius", "0px");
});
