import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

const nestedVariablePath =
  "catalogue-demo/Collections/ANAGRAFICA/tables/ANAGRAFICA/variables/DATADECESSO";

const flatTableKeys = encodeURIComponent(
  JSON.stringify({ resource: { id: "ANAGRAFICA" }, name: "ANAGRAFICA" })
);
const flatTablePath = `catalogue-demo/Tables/ANAGRAFICA?keys=${flatTableKeys}`;
const canonicalTablePath =
  "catalogue-demo/Collections/ANAGRAFICA/tables/ANAGRAFICA";

test.describe("nested record detail", () => {
  test("a nested record URL resolves to the right record", async ({ page }) => {
    await page.goto(`${route}${nestedVariablePath}`);
    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      "DATADECESSO"
    );
  });

  test("the flat ?keys= URL redirects to the nested canonical URL", async ({
    page,
  }) => {
    await page.goto(`${route}${flatTablePath}`);
    await expect(page).toHaveURL(`${route}${canonicalTablePath}`);
    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      "ANAGRAFICA"
    );
  });

  test("breadcrumbs render one crumb per URL segment, each a prefix of the next", async ({
    page,
  }) => {
    await page.goto(`${route}${nestedVariablePath}`);

    const urlSegments = nestedVariablePath.split("/").filter(Boolean);

    const crumbLinks = page
      .locator('nav[aria-label="breadcrumb"] ol')
      .nth(1)
      .getByRole("link");
    await expect(crumbLinks).toHaveCount(urlSegments.length);

    const crumbHrefs = await crumbLinks.evaluateAll((links) =>
      links.map((link) => new URL((link as HTMLAnchorElement).href).pathname)
    );
    for (let index = 1; index < crumbHrefs.length; index++) {
      expect(crumbHrefs[index]!.startsWith(crumbHrefs[index - 1]!)).toBe(true);
    }
  });
});
