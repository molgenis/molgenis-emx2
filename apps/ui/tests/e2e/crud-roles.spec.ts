import type { APIRequestContext, Page } from "@playwright/test";
import { request as apiRequest, expect, test } from "@playwright/test";
import playwrightConfig from "../../playwright.config";
import { deleteSchema, gql, RUN_ID, signinAdmin } from "./e2eUtils";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? playwrightConfig?.use?.baseURL
  : "/apps/ui/";

const SCHEMA = `role crud test ${RUN_ID}`;
const SCHEMA_PATH = encodeURIComponent(SCHEMA);
const ROLE_USER = `e2e-role-user-${RUN_ID}`;
const ROLE = "PatientViewer";
const COPIED_ROLE = `${ROLE}_copy`;

let api: APIRequestContext;

test.describe.configure({ mode: "serial" });

test.beforeAll(async () => {
  api = await apiRequest.newContext();
  await signinAdmin(api, route);
  await gql(
    api,
    `${route}api/graphql`,
    `mutation create($name: String) { createSchema(name: $name) { message } }`,
    { name: SCHEMA }
  );
  await gql(
    api,
    `${route}${SCHEMA_PATH}/graphql`,
    `mutation change($tables: [MolgenisTableInput]) {
      change(tables: $tables) { message }
    }`,
    {
      tables: [
        {
          name: "Hospital",
          columns: [{ name: "name", key: 1 }, { name: "city" }],
        },
        {
          name: "Doctor",
          columns: [
            { name: "id", key: 1 },
            { name: "name" },
            { name: "hospital", columnType: "REF", refTableName: "Hospital" },
          ],
        },
        {
          name: "Patient",
          columns: [
            { name: "id", key: 1 },
            { name: "name" },
            { name: "doctor", columnType: "REF", refTableName: "Doctor" },
          ],
        },
      ],
    }
  );
  await gql(
    api,
    `${route}api/graphql`,
    `mutation { changePassword(email: "${ROLE_USER}", password: "e2e-role-user") { message } }`
  );
});

test.afterAll(async () => {
  await deleteSchema(api, route, SCHEMA);
  await gql(
    api,
    `${route}api/graphql`,
    `mutation { removeUser(email: "${ROLE_USER}") { message } }`
  ).catch((error) => console.error(`could not remove ${ROLE_USER}:`, error));
  await api.dispose();
});

test.beforeEach(async ({ page }) => {
  await signinAdmin(page.request, route);
  await page.goto(`${route}admin/roles`);
  await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
});

async function chooseOption(page: Page, fieldId: string, option: string) {
  await page.locator(`[id="${fieldId}"]`).click();
  await page.locator(`[id="listbox-${fieldId}-options-search"]`).fill(option);
  await page
    .locator(`[id="listbox-${fieldId}-panel"]`)
    .getByRole("option", { name: option, exact: true })
    .click();
  await page.locator("#modal-title").click();
  await expect(page.locator(`[id="listbox-${fieldId}-panel"]`)).toBeHidden();
}

function roleRow(page: Page, role: string) {
  return page
    .getByRole("row")
    .filter({ hasText: SCHEMA })
    .filter({ has: page.getByRole("cell", { name: role, exact: true }) });
}

async function searchRoles(page: Page) {
  await page.getByRole("searchbox", { name: "Search roles" }).fill(SCHEMA);
}

test("role crud", async ({ page }) => {
  await page.getByRole("button", { name: "Create role" }).click();
  await page.locator("#role-modal-name-input").fill(ROLE);
  await chooseOption(page, "role-modal-schema-input", SCHEMA);
  await page.getByRole("checkbox", { name: "Select Patient" }).check();
  await page
    .getByRole("checkbox", { name: "Select all permissions for Doctor" })
    .check();
  await chooseOption(page, "role-modal-users-input", ROLE_USER);
  await page.getByRole("button", { name: "Save" }).click();

  await searchRoles(page);
  const createdRow = roleRow(page, ROLE);
  await expect(createdRow).toHaveCount(1);
  await expect(createdRow).toContainText("Patient");
  await expect(createdRow).toContainText("Doctor");
  await expect(createdRow).not.toContainText("Hospital");
  await expect(createdRow).toContainText(ROLE_USER);

  await createdRow.hover();
  await page
    .getByRole("button", { name: `Edit role: ${ROLE} (${SCHEMA})` })
    .click();
  await expect(
    page.getByRole("checkbox", { name: "Update Doctor" })
  ).toBeChecked();
  await page.getByRole("checkbox", { name: "Row level Patient" }).check();
  await page.getByRole("button", { name: `Remove ${ROLE_USER}` }).click();
  await page.getByRole("button", { name: "Save" }).click();

  await searchRoles(page);
  await expect(roleRow(page, ROLE)).not.toContainText(ROLE_USER);

  await roleRow(page, ROLE).hover();
  await page
    .getByRole("button", { name: `Copy role: ${ROLE} (${SCHEMA})` })
    .click();
  await expect(page.locator("#role-modal-name-input")).toHaveValue(COPIED_ROLE);
  await expect(
    page.getByRole("checkbox", { name: "Select Patient" })
  ).toBeChecked();
  await expect(
    page.getByRole("checkbox", { name: "Row level Patient" })
  ).toBeChecked();
  await expect(
    page.getByRole("checkbox", { name: "Delete Doctor" })
  ).toBeChecked();
  await expect(
    page.getByRole("checkbox", { name: "Select Hospital" })
  ).not.toBeChecked();
  await page.getByRole("button", { name: "Save" }).click();

  await searchRoles(page);
  await expect(roleRow(page, COPIED_ROLE)).toHaveCount(1);

  await roleRow(page, ROLE).getByRole("checkbox").check();
  await roleRow(page, COPIED_ROLE).getByRole("checkbox").check();
  await page.getByRole("button", { name: "Delete selection" }).click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();

  await searchRoles(page);
  await expect(roleRow(page, ROLE)).toHaveCount(0);
  await expect(roleRow(page, COPIED_ROLE)).toHaveCount(0);
});

test("role name validation", async ({ page }) => {
  await gql(
    api,
    `${route}${SCHEMA_PATH}/graphql`,
    `mutation change($roles: [MolgenisRoleInput]) {
      change(roles: $roles) { message }
    }`,
    { roles: [{ name: "DoctorEditor", permissions: [] }] }
  );
  await page.goto(`${route}admin/roles`);

  await page.getByRole("button", { name: "Create role" }).click();
  await chooseOption(page, "role-modal-schema-input", SCHEMA);
  const nameInput = page.locator("#role-modal-name-input");
  const save = page.getByRole("button", { name: "Save" });

  await nameInput.fill("Viewer");
  await expect(page.getByText("'Viewer' is a system role name")).toBeVisible();
  await expect(save).toBeDisabled();

  await nameInput.fill("RLS_patients");
  await expect(
    page.getByText("Role names cannot start with 'RLS_'")
  ).toBeVisible();
  await expect(save).toBeDisabled();

  await nameInput.fill("DoctorEditor");
  await expect(
    page.getByText(`Role 'DoctorEditor' already exists in schema '${SCHEMA}'`)
  ).toBeVisible();
  await expect(save).toBeDisabled();

  await nameInput.fill("NurseViewer");
  await expect(save).toBeEnabled();
});
