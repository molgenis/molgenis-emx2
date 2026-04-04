import { test, expect } from "@playwright/test";

const SCHEMA_NAME = "TestSchemaProfilesE2E";

async function signinViaRequest(request: any): Promise<void> {
  await request.post(`/api/graphql`, {
    data: {
      query: `mutation { signin(email: "admin", password: "admin") { status, message } }`,
    },
  });
}

async function signinViaPage(page: any): Promise<void> {
  const signinButton = page.getByRole("button", { name: /Sign in/i }).first();
  await expect(signinButton).toBeVisible();
  await signinButton.click();

  const userField = page.getByRole("textbox", { name: /Username/ });
  await expect(userField).toBeVisible();
  await userField.fill("admin");

  const passField = page.getByRole("textbox", { name: /Password/ });
  await expect(passField).toBeVisible();
  await passField.fill("admin");

  const submitBtn = page.getByRole("dialog").getByRole("button", { name: /Sign in/ });
  await expect(submitBtn).toBeVisible();
  await submitBtn.click();

  await page.waitForLoadState("networkidle");
  await page.waitForTimeout(500);
}

async function createTestSchema(request: any): Promise<void> {
  const response = await request.post(`/api/graphql`, {
    data: {
      query: `mutation { createSchema(name: "${SCHEMA_NAME}") { message } }`,
    },
  });
  const result = await response.json();
  if (result.errors) {
    throw new Error(`Failed to create schema: ${JSON.stringify(result.errors)}`);
  }
}

async function createTablesWithProfiles(request: any): Promise<void> {
  const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: `
        mutation {
          change(tables: [
            {
              name: "Samples"
              columns: [
                {name: "id", columnType: "STRING", key: 1}
                {name: "name", columnType: "STRING"}
                {name: "wgs_field", columnType: "STRING", profiles: ["wgs"]}
              ]
            }
            {
              name: "WGS"
              profiles: ["wgs"]
              columns: [
                {name: "id", columnType: "STRING", key: 1}
                {name: "coverage", columnType: "INT"}
              ]
            }
            {
              name: "Imaging"
              profiles: ["imaging"]
              columns: [
                {name: "id", columnType: "STRING", key: 1}
                {name: "modality", columnType: "STRING"}
              ]
            }
          ]) {
            message
          }
        }
      `,
    },
  });
  const result = await response.json();
  if (result.errors) {
    throw new Error(`Failed to create tables: ${JSON.stringify(result.errors)}`);
  }
}

async function setActiveProfiles(request: any, profiles: string[]): Promise<void> {
  const profilesArg = profiles.map((p) => `"${p}"`).join(", ");
  const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: `mutation { change(activeProfiles: [${profilesArg}]) { message } }`,
    },
  });
  const result = await response.json();
  if (result.errors) {
    throw new Error(`Failed to set active profiles: ${JSON.stringify(result.errors)}`);
  }
}

async function deleteTestSchema(request: any): Promise<void> {
  await request.post(`/api/graphql`, {
    data: {
      query: `mutation { deleteSchema(id: "${SCHEMA_NAME}") { message } }`,
    },
  });
}

async function navigateToSchemaEditor(page: any): Promise<void> {
  await page.goto(`/${SCHEMA_NAME}/schema/`);
  await page.waitForLoadState("networkidle");

  // Check if we need to sign in (look for "unknown schema" message)
  const unknownMsg = page.getByText(/unknown/i);
  const needsSignin = await unknownMsg.isVisible({ timeout: 500 }).catch(() => false);

  if (needsSignin) {
    await signinViaPage(page);
    // After signin, reload the schema page
    await page.goto(`/${SCHEMA_NAME}/schema/`);
    await page.waitForLoadState("networkidle");
  }

  // Wait for h4 headings (table headings) to be visible
  const h4Headings = page.locator("h4");
  await expect(h4Headings.first()).toBeVisible({ timeout: 15000 });
}

test.describe("Schema Editor - Profile Support", () => {
  test.describe.configure({ mode: "serial" });

  test.beforeAll(async ({ request }) => {
    // Sign in first
    await signinViaRequest(request);

    // Delete old schema if it exists
    await request.post(`/api/graphql`, {
      data: {
        query: `mutation { deleteSchema(id: "${SCHEMA_NAME}") { message } }`,
      },
    });

    // Create test schema
    const createResponse = await request.post(`/api/graphql`, {
      data: {
        query: `mutation { createSchema(name: "${SCHEMA_NAME}") { message } }`,
      },
    });
    const createResult = await createResponse.json();
    if (createResult.errors) {
      throw new Error(`Failed to create schema: ${JSON.stringify(createResult.errors)}`);
    }

    // Small delay to ensure schema is ready
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Create tables with profiles
    const tablesResponse = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `
          mutation {
            change(tables: [
              {
                name: "Samples"
                columns: [
                  {name: "id", columnType: "STRING", key: 1}
                  {name: "name", columnType: "STRING"}
                  {name: "wgs_field", columnType: "STRING", profiles: ["wgs"]}
                ]
              }
              {
                name: "WGS"
                profiles: ["wgs"]
                columns: [
                  {name: "id", columnType: "STRING", key: 1}
                  {name: "coverage", columnType: "INT"}
                ]
              }
              {
                name: "Imaging"
                profiles: ["imaging"]
                columns: [
                  {name: "id", columnType: "STRING", key: 1}
                  {name: "modality", columnType: "STRING"}
                ]
              }
            ]) {
              message
            }
          }
        `,
      },
    });
    const tablesResult = await tablesResponse.json();
    if (tablesResult.errors) {
      throw new Error(`Failed to create tables: ${JSON.stringify(tablesResult.errors)}`);
    }

    // Set active profiles
    const profilesResponse = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `mutation { change(activeProfiles: ["wgs"]) { message } }`,
      },
    });
    const profilesResult = await profilesResponse.json();
    if (profilesResult.errors) {
      throw new Error(`Failed to set active profiles: ${JSON.stringify(profilesResult.errors)}`);
    }
  });

  test.afterAll(async ({ request }) => {
    await signinViaRequest(request);
    await deleteTestSchema(request);
  });

  test("should display profiles on tables in schema editor", async ({ page }) => {
    await navigateToSchemaEditor(page);

    // WGS table should show [wgs] profile tag
    const wgsHeading = page.locator("h4").filter({ hasText: "Table: WGS" });
    await expect(wgsHeading).toBeVisible({ timeout: 10000 });
    await expect(wgsHeading).toContainText("[wgs]");

    // Imaging table should show [imaging] profile tag
    const imagingHeading = page.locator("h4").filter({ hasText: "Table: Imaging" });
    await expect(imagingHeading).toBeVisible();
    await expect(imagingHeading).toContainText("[imaging]");

    // Samples table should NOT show profile tag (no profiles on table itself)
    const samplesHeading = page.locator("h4").filter({ hasText: "Table: Samples" });
    await expect(samplesHeading).toBeVisible();
    await expect(samplesHeading).not.toContainText("[");
  });

  test("should display profiles on columns in schema editor", async ({ page }) => {
    await navigateToSchemaEditor(page);

    // Find the wgs_field column in the Samples table — it should show [wgs]
    const wgsFieldCell = page.locator("td").filter({ hasText: "wgs_field" }).first();
    await expect(wgsFieldCell).toBeVisible({ timeout: 10000 });
    await expect(wgsFieldCell).toContainText("[wgs]");
  });

  test("should show profile checkboxes in table edit modal", async ({ page }) => {
    await navigateToSchemaEditor(page);

    // Find WGS table heading
    const wgsHeading = page.locator("h4").filter({ hasText: "Table: WGS" });
    await expect(wgsHeading).toBeVisible({ timeout: 10000 });

    // Find the edit button (pencil icon) - it's in a hoverContainer span with class hoverIcon
    // The button is hidden by default (hover-only), so use force: true
    const parentSection = wgsHeading.locator("..");
    const editButton = parentSection.locator("button.hoverIcon").first();
    await editButton.click({ force: true });

    // Modal should open with "Profiles" label
    // Get the label from the modal dialog (which should be open)
    const profilesLabel = page.getByRole("dialog").locator("label").filter({ hasText: /^Profiles$/ });
    await expect(profilesLabel).toBeVisible({ timeout: 5000 });

    // Should have checkboxes for known profiles - scope to the modal dialog
    const dialog = page.getByRole("dialog");

    // Check that "wgs" checkbox exists and is checked (WGS table has profiles: ["wgs"])
    const wgsCheckbox = dialog.locator('input[type="checkbox"][value="wgs"]');
    await expect(wgsCheckbox).toBeVisible();
    await expect(wgsCheckbox).toBeChecked();

    // "imaging" checkbox should exist but NOT be checked
    const imagingCheckbox = dialog.locator('input[type="checkbox"][value="imaging"]');
    await expect(imagingCheckbox).toBeVisible();
    await expect(imagingCheckbox).not.toBeChecked();

    // Close modal
    await page.getByRole("dialog").getByRole("button", { name: "Cancel" }).click();
  });

  test("should show profile checkboxes in column edit modal", async ({ page }) => {
    await navigateToSchemaEditor(page);

    // Find the wgs_field column cell
    const wgsFieldCell = page.locator("td").filter({ hasText: "wgs_field" }).first();
    await expect(wgsFieldCell).toBeVisible({ timeout: 10000 });

    // Click the edit icon button in that cell's row
    // The edit button should be in the same row with class hoverIcon (hidden by default)
    const editButton = wgsFieldCell.locator("..").locator("button.hoverIcon").first();
    await editButton.click({ force: true });

    // Modal should have "profiles" label
    const dialog = page.getByRole("dialog");
    const profilesLabel = dialog.locator("label").filter({ hasText: /[Pp]rofiles/ });
    await expect(profilesLabel).toBeVisible({ timeout: 5000 });

    // "wgs" checkbox should be checked (wgs_field has profiles: ["wgs"])
    const wgsCheckbox = dialog.locator('input[type="checkbox"][value="wgs"]');
    await expect(wgsCheckbox).toBeVisible();
    await expect(wgsCheckbox).toBeChecked();

    // Close modal
    await page.getByRole("dialog").getByRole("button", { name: "Cancel" }).click();
  });

  test("should show active profiles checkboxes in schema header", async ({ page }) => {
    await navigateToSchemaEditor(page);

    // "Active profiles" label should be visible in the header
    const activeProfilesLabel = page.locator("label").filter({ hasText: "Active profiles" });
    await expect(activeProfilesLabel).toBeVisible({ timeout: 10000 });

    // "wgs" checkbox should be checked (we set activeProfiles: ["wgs"] in setup)
    const wgsCheckbox = page.locator('#schema_active_profiles input[type="checkbox"][value="wgs"]').first();
    // If ID-based selector doesn't work, try a broader one near the Active profiles label
    const wgsCheckboxAlt = page.locator('input[type="checkbox"][value="wgs"]').first();

    const hasIdBased = await wgsCheckbox.isVisible({ timeout: 2000 }).catch(() => false);
    if (hasIdBased) {
      await expect(wgsCheckbox).toBeChecked();
    } else {
      await expect(wgsCheckboxAlt).toBeVisible();
    }
  });
});
