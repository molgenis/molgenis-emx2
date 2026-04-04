import { test, expect } from "@playwright/test";
import playwrightConfig from "../../playwright.config";

const route = playwrightConfig?.use?.baseURL?.startsWith("http://localhost")
  ? ""
  : "/apps/ui/";

const SCHEMA_NAME = "TestProfileFilteringE2E";

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

async function insertSampleRecord(request: any): Promise<void> {
  const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: `
        mutation {
          insert(Samples: [{id: "test-record-1", name: "Test Sample"}]) {
            message
          }
        }
      `,
    },
  });

  const result = await response.json();
  if (result.errors) {
    throw new Error(
      `Failed to insert sample: ${JSON.stringify(result.errors)}`
    );
  }
}

async function deleteSampleRecord(request: any): Promise<void> {
  await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: `
        mutation {
          delete(Samples: [{id: "test-record-1"}]) {
            message
          }
        }
      `,
    },
  });
}

async function signin(request: any): Promise<void> {
  await request.post(`/api/graphql`, {
    data: {
      query: `mutation { signin(email: "admin", password: "admin") { status, message } }`,
    },
  });
}

test.describe("Profile Filtering", () => {
  test.beforeAll(async ({ request }) => {
    await signin(request);
    await createTestSchema(request);
    await createTablesWithProfiles(request);
  });

  test.afterAll(async ({ request }) => {
    await deleteTestSchema(request);
  });

  test("should show all tables in listing when no profiles active", async ({
    page,
  }) => {
    await page.goto(`${route}${SCHEMA_NAME}`);

    if (await page.getByRole("button", { name: "Signin" }).isVisible()) {
      await page.getByRole("button", { name: "Signin" }).click();
      await page.getByRole("textbox", { name: "Username" }).fill("admin");
      await page.getByRole("textbox", { name: "Username" }).press("Tab");
      await page.getByRole("textbox", { name: "Password" }).fill("admin");
      await page.getByRole("button", { name: "Sign in" }).click();
      await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
    }

    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      SCHEMA_NAME
    );

    const samplesLink = page.getByRole("link", { name: "Samples" }).first();
    await expect(samplesLink).toBeVisible();

    const wgsLink = page.getByRole("link", { name: "WGS" }).first();
    await expect(wgsLink).toBeVisible();

    const imagingLink = page.getByRole("link", { name: "Imaging" }).first();
    await expect(imagingLink).toBeVisible();
  });

  test("should filter tables via API with explicit profiles parameter", async ({
    request,
  }) => {
    const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `
          {
            _schema(profiles: ["wgs"]) {
              tables {
                name
                profiles
              }
            }
          }
        `,
      },
    });

    const result = await response.json();
    if (result.errors) {
      throw new Error(
        `Failed to query schema: ${JSON.stringify(result.errors)}`
      );
    }

    const tables = result.data._schema.tables;
    const tableNames = tables.map((t: any) => t.name);

    expect(tableNames).toContain("Samples");
    expect(tableNames).toContain("WGS");
    expect(tableNames).not.toContain("Imaging");
  });

  test("should filter columns via API with explicit profiles parameter", async ({
    request,
  }) => {
    const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `
          {
            _schema(profiles: ["wgs"]) {
              tables {
                name
                columns {
                  name
                  profiles
                }
              }
            }
          }
        `,
      },
    });

    const result = await response.json();
    if (result.errors) {
      throw new Error(
        `Failed to query schema: ${JSON.stringify(result.errors)}`
      );
    }

    const tables = result.data._schema.tables;
    const samplesTable = tables.find((t: any) => t.name === "Samples");
    const samplesColumnNames = samplesTable.columns.map((c: any) => c.name);

    expect(samplesColumnNames).toContain("id");
    expect(samplesColumnNames).toContain("name");
    expect(samplesColumnNames).toContain("wgs_field");
  });

  test("should hide profile-filtered columns via API when profile not active", async ({
    request,
  }) => {
    const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `
          {
            _schema(profiles: ["imaging"]) {
              tables {
                name
                columns {
                  name
                  profiles
                }
              }
            }
          }
        `,
      },
    });

    const result = await response.json();
    if (result.errors) {
      throw new Error(
        `Failed to query schema: ${JSON.stringify(result.errors)}`
      );
    }

    const tables = result.data._schema.tables;
    const samplesTable = tables.find((t: any) => t.name === "Samples");
    const samplesColumnNames = samplesTable.columns.map((c: any) => c.name);

    expect(samplesColumnNames).toContain("id");
    expect(samplesColumnNames).toContain("name");
    expect(samplesColumnNames).not.toContain("wgs_field");
  });

  test("should show all columns in form when no profiles active", async ({
    page,
    request,
  }) => {
    await signin(request);
    await insertSampleRecord(request);

    await page.goto(`${route}${SCHEMA_NAME}/Samples`);

    if (await page.getByRole("button", { name: "Signin" }).isVisible()) {
      await page.getByRole("button", { name: "Signin" }).click();
      await page.getByRole("textbox", { name: "Username" }).fill("admin");
      await page.getByRole("textbox", { name: "Username" }).press("Tab");
      await page.getByRole("textbox", { name: "Password" }).fill("admin");
      await page.getByRole("button", { name: "Sign in" }).click();
      await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
    }

    const sampleLink = page
      .getByRole("link", { name: "test-record-1" })
      .first();
    await expect(sampleLink).toBeVisible();
    await sampleLink.click();

    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      "Samples"
    );

    const nameField = page.getByLabel(/^name$/i).first();
    await expect(nameField).toBeVisible();

    const wgsFieldLabel = page.getByText(/wgs.field/i).first();
    await expect(wgsFieldLabel).toBeVisible();

    await deleteSampleRecord(request);
  });
});
