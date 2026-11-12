import type { APIRequestContext, Locator, Page } from "@playwright/test";
import { request as apiRequest, expect, test } from "@playwright/test";
import playwrightConfig from "../../playwright.config";
import {
  addPasswordToUser,
  createSchemaFromTemplate,
  deleteSchema,
  gql,
  RUN_ID,
  signinAdmin,
} from "./e2eUtils";
import { signin, signout } from "./testUtils/testUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

const SCHEMA = `theme test ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
const SCHEMA_THEME_PAGE = `${route}${SCHEMA_PATH}/theme`;
const MANAGER = `theme-manager-${RUN_ID}`;
const VIEWER = `theme-viewer-${RUN_ID}`;
const PASSWORD = "theme-test-password";
const CUSTOM_THEME = `Brand ${RUN_ID}`;
const COLOR_VARIABLE = "--color-blue-500";

let api: APIRequestContext;

test.describe.configure({ mode: "serial" });

async function clearApplicationTheme() {
  await gql(
    api,
    `${route}api/graphql`,
    `mutation drop($settings: [DropSettingsInput]) {
      drop(settings: $settings) { message }
    }`,
    { settings: [{ key: "tw-theme" }, { key: "tw-custom-themes" }] }
  );
}

function themeRow(page: Page, name: string): Locator {
  return page
    .getByRole("listitem")
    .filter({ has: page.getByText(name, { exact: true }) });
}

function html(page: Page): Locator {
  return page.locator("html");
}

function inlineVariable(page: Page, name: string): Promise<string> {
  return page.evaluate(
    (variable) =>
      document.documentElement.style.getPropertyValue(variable).trim(),
    name
  );
}

async function useTheme(page: Page, name: string) {
  const row = themeRow(page, name);
  await row.getByRole("button", { name: "Use", exact: true }).click();
  await expect(row.getByText("(active)")).toBeVisible();
}

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await signinAdmin(api, route as string);
  await createSchemaFromTemplate(api, route as string, SCHEMA, "PET_STORE");
  await gql(
    api,
    `${route}${SCHEMA_PATH}/graphql`,
    `mutation change($members: [MolgenisMembersInput]) {
      change(members: $members) { message }
    }`,
    {
      members: [
        { email: MANAGER, role: "Manager" },
        { email: VIEWER, role: "Viewer" },
      ],
    }
  );
  await addPasswordToUser(api, route as string, MANAGER, PASSWORD);
  await addPasswordToUser(api, route as string, VIEWER, PASSWORD);
});

test.afterAll(async () => {
  await clearApplicationTheme().catch((error) =>
    console.error("could not clear application theme", error)
  );
  await deleteSchema(api, route as string, SCHEMA);
  for (const email of [MANAGER, VIEWER]) {
    await gql(
      api,
      `${route}api/graphql`,
      `mutation removeUser($email: String) {
        removeUser(email: $email) { message }
      }`,
      { email }
    ).catch((error) => console.error(`could not remove ${email}`, error));
  }
  await api.dispose();
});

test.describe("as admin", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto(`${route}admin/theme`);
    await signin(page, "admin", "admin");
    await page.goto(`${route}admin/theme`);
    await expect(
      page.getByRole("heading", { name: "Application theme" })
    ).toBeVisible();
  });

  test("selects a built-in theme for the application", async ({ page }) => {
    await useTheme(page, "UMCG");
    await page.reload();
    await expect(html(page)).toHaveAttribute("data-theme", "umcg");
    await expect(
      page.getByRole("button", { name: "Toggle theme" })
    ).toHaveCount(0);

    await page.getByRole("button", { name: "Let visitors choose" }).click();
    await expect(
      page.getByRole("button", { name: "Toggle theme" })
    ).toBeVisible();
  });

  test("built-in themes cannot be edited or deleted", async ({ page }) => {
    const row = themeRow(page, "Dark");
    await expect(row.getByRole("button", { name: "Edit" })).toHaveCount(0);
    await expect(row.getByRole("button", { name: "Delete" })).toHaveCount(0);
  });

  test("rejects a built-in theme name", async ({ page }) => {
    await themeRow(page, "Dark")
      .getByRole("button", { name: "Duplicate and customise" })
      .click();
    await page.getByLabel("Name", { exact: true }).fill("molgenis");
    await page.getByRole("button", { name: "Save theme" }).click();
    await expect(
      page.getByText("This name is reserved for a built-in theme")
    ).toBeVisible();
  });

  test("creates, applies and edits a custom theme", async ({ page }) => {
    await themeRow(page, "MOLGENIS")
      .getByRole("button", { name: "Duplicate and customise" })
      .click();
    await page.getByLabel("Name", { exact: true }).fill(CUSTOM_THEME);
    await page.getByLabel("Search variables").fill(COLOR_VARIABLE);
    await page.getByLabel(COLOR_VARIABLE, { exact: true }).fill("#112233");
    await expect
      .poll(() => inlineVariable(page, COLOR_VARIABLE))
      .toBe("#112233");
    await page.getByRole("button", { name: "Save theme" }).click();

    await useTheme(page, CUSTOM_THEME);
    await page.reload();
    await expect(html(page)).toHaveAttribute("data-theme", "molgenis");
    await expect
      .poll(() => inlineVariable(page, COLOR_VARIABLE))
      .toBe("#112233");

    await themeRow(page, CUSTOM_THEME)
      .getByRole("button", { name: "Edit" })
      .click();
    await page.getByLabel("Search variables").fill(COLOR_VARIABLE);
    await page.getByLabel(COLOR_VARIABLE, { exact: true }).fill("#445566");
    await page.getByRole("button", { name: "Save theme" }).click();
    await expect(themeRow(page, CUSTOM_THEME)).toBeVisible();
    await page.reload();
    await expect
      .poll(() => inlineVariable(page, COLOR_VARIABLE))
      .toBe("#445566");

    await expect(
      themeRow(page, CUSTOM_THEME).getByRole("button", { name: "Delete" })
    ).toBeDisabled();
  });

  test("cancelling an edit restores the saved theme", async ({ page }) => {
    await themeRow(page, CUSTOM_THEME)
      .getByRole("button", { name: "Edit" })
      .click();
    await page.getByLabel("Search variables").fill(COLOR_VARIABLE);
    await page.getByLabel(COLOR_VARIABLE, { exact: true }).fill("#778899");
    await expect
      .poll(() => inlineVariable(page, COLOR_VARIABLE))
      .toBe("#778899");
    await page.getByRole("button", { name: "Cancel" }).click();
    await expect
      .poll(() => inlineVariable(page, COLOR_VARIABLE))
      .toBe("#445566");
  });

  test("a schema theme beats the application theme", async ({ page }) => {
    await page.goto(SCHEMA_THEME_PAGE);
    await expect(themeRow(page, CUSTOM_THEME)).toBeVisible();
    await expect(
      themeRow(page, CUSTOM_THEME).getByRole("button", { name: "Edit" })
    ).toHaveCount(0);

    await useTheme(page, "Dark");
    await page.reload();
    await expect(html(page)).toHaveAttribute("data-theme", "dark");
    await expect.poll(() => inlineVariable(page, COLOR_VARIABLE)).toBe("");

    await page.goto(`${route}admin/theme`);
    await expect(html(page)).toHaveAttribute("data-theme", "molgenis");

    await page.goto(SCHEMA_THEME_PAGE);
    await page
      .getByRole("button", { name: "Inherit application theme" })
      .click();
    await expect(html(page)).toHaveAttribute("data-theme", "molgenis");
  });

  test("the query parameter recovers from a custom theme", async ({ page }) => {
    await page.goto(`${route}admin/theme?theme=light`);
    await expect(html(page)).toHaveAttribute("data-theme", "light");
    await expect.poll(() => inlineVariable(page, COLOR_VARIABLE)).toBe("");
  });
});

test.describe("as a schema user", () => {
  test("a visitor who is not signed in sees the configured theme", async ({
    page,
  }) => {
    await page.goto(`${route}${SCHEMA_PATH}`);
    await expect(html(page)).toHaveAttribute("data-theme", "molgenis");
    await expect
      .poll(() => inlineVariable(page, COLOR_VARIABLE))
      .toBe("#445566");
  });

  test("a Manager can change the schema theme", async ({ page }) => {
    await page.goto(`${route}${SCHEMA_PATH}`);
    await signin(page, MANAGER, PASSWORD);
    await page.goto(`${route}${SCHEMA_PATH}`);
    await page.getByRole("button", { name: "More" }).first().click();
    await page.getByRole("link", { name: "Theme", exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`${SCHEMA_PATH}/theme$`));

    await useTheme(page, "AUMC");
    await page.reload();
    await expect(html(page)).toHaveAttribute("data-theme", "aumc");
    await signout(page);
  });

  test("a Viewer cannot change the schema theme", async ({ page }) => {
    await page.goto(`${route}${SCHEMA_PATH}`);
    await signin(page, VIEWER, PASSWORD);
    await page.goto(SCHEMA_THEME_PAGE);
    await expect(
      page.getByText("You need at least the Manager role")
    ).toBeVisible();
    await expect(page.getByRole("button", { name: "Use" })).toHaveCount(0);
    await signout(page);
  });
});
