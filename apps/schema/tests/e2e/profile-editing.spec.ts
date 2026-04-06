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

  const submitBtn = page
    .getByRole("dialog")
    .getByRole("button", { name: /Sign in/ });
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
    throw new Error(
      `Failed to create schema: ${JSON.stringify(result.errors)}`
    );
  }
}

async function createTablesWithTemplates(request: any): Promise<void> {
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
    throw new Error(
      `Failed to create tables: ${JSON.stringify(result.errors)}`
    );
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

  const unknownMsg = page.getByText(/unknown/i);
  const needsSignin = await unknownMsg
    .isVisible({ timeout: 500 })
    .catch(() => false);

  if (needsSignin) {
    await signinViaPage(page);
    await page.goto(`/${SCHEMA_NAME}/schema/`);
    await page.waitForLoadState("networkidle");
  }

  const h4Headings = page.locator("h4");
  await expect(h4Headings.first()).toBeVisible({ timeout: 15000 });
}

test.describe("Schema Editor - Profile Support", () => {
  test.describe.configure({ mode: "serial" });

  test.beforeAll(async ({ request }) => {
    await signinViaRequest(request);

    await request.post(`/api/graphql`, {
      data: {
        query: `mutation { deleteSchema(id: "${SCHEMA_NAME}") { message } }`,
      },
    });

    const createResponse = await request.post(`/api/graphql`, {
      data: {
        query: `mutation { createSchema(name: "${SCHEMA_NAME}") { message } }`,
      },
    });
    const createResult = await createResponse.json();
    if (createResult.errors) {
      throw new Error(
        `Failed to create schema: ${JSON.stringify(createResult.errors)}`
      );
    }

    await new Promise((resolve) => setTimeout(resolve, 1000));

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
                subsets: ["wgs"]
                columns: [
                  {name: "id", columnType: "STRING", key: 1}
                  {name: "coverage", columnType: "INT"}
                ]
              }
              {
                name: "Imaging"
                subsets: ["imaging"]
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
      throw new Error(
        `Failed to create tables: ${JSON.stringify(tablesResult.errors)}`
      );
    }
  });

  test.afterAll(async ({ request }) => {
    await signinViaRequest(request);
    await deleteTestSchema(request);
  });

  test("should display profiles on tables in schema editor", async ({
    page,
  }) => {
    await navigateToSchemaEditor(page);

    const wgsHeading = page.locator("h4").filter({ hasText: "Table: WGS" });
    await expect(wgsHeading).toBeVisible({ timeout: 10000 });
    await expect(wgsHeading).toContainText("[wgs]");

    const imagingHeading = page
      .locator("h4")
      .filter({ hasText: "Table: Imaging" });
    await expect(imagingHeading).toBeVisible();
    await expect(imagingHeading).toContainText("[imaging]");

    const samplesHeading = page
      .locator("h4")
      .filter({ hasText: "Table: Samples" });
    await expect(samplesHeading).toBeVisible();
    await expect(samplesHeading).not.toContainText("[");
  });

  test("should display profiles on columns in schema editor", async ({
    page,
  }) => {
    await navigateToSchemaEditor(page);

    const wgsFieldCell = page
      .locator("td")
      .filter({ hasText: "wgs_field" })
      .first();
    await expect(wgsFieldCell).toBeVisible({ timeout: 10000 });
    await expect(wgsFieldCell).toContainText("[wgs]");
  });

  test("should show profile checkboxes in table edit modal", async ({
    page,
  }) => {
    await navigateToSchemaEditor(page);

    const wgsHeading = page.locator("h4").filter({ hasText: "Table: WGS" });
    await expect(wgsHeading).toBeVisible({ timeout: 10000 });

    const parentSection = wgsHeading.locator("..");
    const editButton = parentSection.locator("button.hoverIcon").first();
    await editButton.click({ force: true });

    const profilesLabel = page
      .getByRole("dialog")
      .locator("label")
      .filter({ hasText: /^Profiles$/ });
    await expect(profilesLabel).toBeVisible({ timeout: 5000 });

    const dialog = page.getByRole("dialog");

    const wgsCheckbox = dialog.locator('input[type="checkbox"][value="wgs"]');
    await expect(wgsCheckbox).toBeVisible();
    await expect(wgsCheckbox).toBeChecked();

    const imagingCheckbox = dialog.locator(
      'input[type="checkbox"][value="imaging"]'
    );
    await expect(imagingCheckbox).toBeVisible();
    await expect(imagingCheckbox).not.toBeChecked();

    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Cancel" })
      .click();
  });

  test("should show profile checkboxes in column edit modal", async ({
    page,
  }) => {
    await navigateToSchemaEditor(page);

    const wgsFieldCell = page
      .locator("td")
      .filter({ hasText: "wgs_field" })
      .first();
    await expect(wgsFieldCell).toBeVisible({ timeout: 10000 });

    const editButton = wgsFieldCell
      .locator("..")
      .locator("button.hoverIcon")
      .first();
    await editButton.click({ force: true });

    const dialog = page.getByRole("dialog");
    const profilesLabel = dialog
      .locator("label")
      .filter({ hasText: /[Pp]rofiles/ });
    await expect(profilesLabel).toBeVisible({ timeout: 5000 });

    const wgsCheckbox = dialog.locator('input[type="checkbox"][value="wgs"]');
    await expect(wgsCheckbox).toBeVisible();
    await expect(wgsCheckbox).toBeChecked();

    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Cancel" })
      .click();
  });

  test("should manage profile activation in schema header for bundle-backed schema", async ({
    page,
    request,
  }) => {
    await signinViaRequest(request);

    const bundleSchemaName = "TestBundleSchemaE2E";

    await request.post(`/api/graphql`, {
      data: {
        query: `mutation { deleteSchema(id: "${bundleSchemaName}") { message } }`,
      },
    });

    const yamlContent = `name: test-bundle
subsets:
  wgs:
    description: "Whole genome sequencing"
  imaging:
    description: "Medical imaging data"
tables:
  - name: Samples
    columns:
      - name: id
        columnType: STRING
        key: 1
      - name: name
        columnType: STRING
  - name: WGS
    subsets: [wgs]
    columns:
      - name: id
        columnType: STRING
        key: 1
      - name: coverage
        columnType: INT
  - name: Imaging
    subsets: [imaging]
    columns:
      - name: id
        columnType: STRING
        key: 1
      - name: modality
        columnType: STRING`;

    const formData = new FormData();
    formData.append(
      "file",
      new Blob([yamlContent], { type: "text/yaml" }),
      "test-bundle.yaml"
    );
    formData.append("name", bundleSchemaName);

    await request.post(`/api/import/${bundleSchemaName}`, {
      multipart: {
        file: {
          name: "test-bundle.yaml",
          mimeType: "text/yaml",
          buffer: Buffer.from(yamlContent),
        },
        name: bundleSchemaName,
      },
    });

    await navigateToSchemaEditor(page);
    await page.goto(`/${bundleSchemaName}/schema/`);
    await page.waitForLoadState("networkidle");

    const bundleNameLabel = page.locator(".subsets-panel__header strong");
    await expect(bundleNameLabel).toBeVisible({ timeout: 10000 });
    await expect(bundleNameLabel).toContainText("test-bundle");

    const profilesLabel = page
      .locator(".subsets-panel__label")
      .filter({ hasText: "Active profiles" });
    await expect(profilesLabel).toBeVisible();

    const wgsCheckbox = page
      .locator('input[type="checkbox"]')
      .filter({ has: page.locator('id="subset_wgs"') });
    const imagingCheckbox = page
      .locator('input[type="checkbox"]')
      .filter({ has: page.locator('id="subset_imaging"') });

    await expect(wgsCheckbox).toBeVisible();
    await expect(imagingCheckbox).toBeVisible();

    await signinViaRequest(request);
    await request.post(`/api/graphql`, {
      data: {
        query: `mutation { deleteSchema(id: "${bundleSchemaName}") { message } }`,
      },
    });
  });
});
