import { test, expect, Page } from "@playwright/test";

const SCHEMA_NAME = "TestInheritanceUI";
const BASE_URL = process.env.E2E_BASE_URL || "https://emx2.dev.molgenis.org/";

async function createTestSchema(request: any): Promise<void> {
  const response = await request.post(`/api/graphql`, {
    data: {
      query: `
        mutation {
          change {
            createSchema(name: "${SCHEMA_NAME}") {
              message
            }
          }
        }
      `,
    },
  });

  const result = await response.json();
  if (result.errors) {
    throw new Error(`Failed to create schema: ${JSON.stringify(result.errors)}`);
  }
}

async function deleteTestSchema(request: any): Promise<void> {
  await request.post(`/api/graphql`, {
    data: {
      query: `
        mutation {
          change {
            deleteSchema(id: "${SCHEMA_NAME}") {
              message
            }
          }
        }
      `,
    },
  });
}

async function createTablesWithInheritance(request: any): Promise<void> {
  const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: `
        mutation {
          change {
            createTable(name: "Experiments") {
              message
            }
            createTable(name: "sampling") {
              message
            }
            createTable(name: "sequencing") {
              message
            }
            createTable(name: "WGS") {
              message
            }
            createTable(name: "Imaging") {
              message
            }
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

async function configureTableInheritance(request: any): Promise<void> {
  const updateMutation = `
    mutation {
      change {
        updateTable(name: "Experiments", columns: [
          {name: "id", columnType: "STRING", key: 1}
          {name: "experiment_type", columnType: "EXTENSION"}
        ]) {
          message
        }
        updateTable(name: "sampling", tableType: INTERNAL, inheritNames: ["Experiments"], columns: [
          {name: "tissue_type", columnType: "STRING"}
        ]) {
          message
        }
        updateTable(name: "sequencing", tableType: INTERNAL, inheritNames: ["Experiments"], columns: [
          {name: "read_length", columnType: "INT"}
        ]) {
          message
        }
        updateTable(name: "WGS", inheritNames: ["sampling", "sequencing"], columns: [
          {name: "coverage", columnType: "DECIMAL"}
        ]) {
          message
        }
        updateTable(name: "Imaging", inheritNames: ["Experiments"], columns: [
          {name: "modality", columnType: "STRING"}
        ]) {
          message
        }
      }
    }
  `;

  const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: updateMutation,
    },
  });

  const result = await response.json();
  if (result.errors) {
    throw new Error(
      `Failed to configure inheritance: ${JSON.stringify(result.errors)}`
    );
  }
}

async function verifySchemaViaAPI(request: any): Promise<void> {
  const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: `
        {
          _schema {
            name
            tables {
              name
              tableType
              inheritNames
              columns {
                name
                columnType
              }
            }
          }
        }
      `,
    },
  });

  const result = await response.json();
  if (result.errors) {
    throw new Error(`Failed to query schema: ${JSON.stringify(result.errors)}`);
  }

  const schema = result.data?._schema;
  expect(schema.name).toBe(SCHEMA_NAME);

  const tables = schema.tables;
  const experimentsTable = tables.find((t: any) => t.name === "Experiments");
  expect(experimentsTable).toBeDefined();
  expect(experimentsTable.tableType).toBe("DATA");

  const samplingTable = tables.find((t: any) => t.name === "sampling");
  expect(samplingTable).toBeDefined();
  expect(samplingTable.tableType).toBe("INTERNAL");
  expect(samplingTable.inheritNames).toContain("Experiments");

  const sequencingTable = tables.find((t: any) => t.name === "sequencing");
  expect(sequencingTable).toBeDefined();
  expect(sequencingTable.tableType).toBe("INTERNAL");
  expect(sequencingTable.inheritNames).toContain("Experiments");

  const wgsTable = tables.find((t: any) => t.name === "WGS");
  expect(wgsTable).toBeDefined();
  expect(wgsTable.inheritNames).toContain("sampling");
  expect(wgsTable.inheritNames).toContain("sequencing");

  const imagingTable = tables.find((t: any) => t.name === "Imaging");
  expect(imagingTable).toBeDefined();
  expect(imagingTable.inheritNames).toContain("Experiments");

  expect(experimentsTable.columns.find((c: any) => c.name === "experiment_type").columnType).toBe("EXTENSION");
}

test.describe("Schema Editor - Multiple Inheritance UI", () => {
  let page: Page;

  test.beforeAll(async ({ request }) => {
    await createTestSchema(request);
    await createTablesWithInheritance(request);
    await configureTableInheritance(request);
  });

  test.afterAll(async ({ request }) => {
    await deleteTestSchema(request);
  });

  test("should display schema with multi-parent inheritance via API", async ({
    request,
  }) => {
    await verifySchemaViaAPI(request);
  });

  test("should render schema editor with inherited tables", async ({ page }) => {
    const schemaUrl = new URL(`/${SCHEMA_NAME}/schema/`, BASE_URL).toString();
    await page.goto(schemaUrl);
    await expect(page).toHaveTitle(/emx2-schema/i);

    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      SCHEMA_NAME
    );
  });

  test("should display table inheritance information", async ({ page }) => {
    const schemaUrl = new URL(`/${SCHEMA_NAME}/schema/`, BASE_URL).toString();
    await page.goto(schemaUrl);

    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      SCHEMA_NAME
    );

    const tables = ["Experiments", "sampling", "sequencing", "WGS", "Imaging"];
    for (const tableName of tables) {
      const tableElement = page.getByRole("cell", { name: tableName }).first();
      await expect(tableElement).toBeVisible();
    }
  });

  test("should show INTERNAL table type for sampling and sequencing", async ({
    page,
  }) => {
    const schemaUrl = new URL(`/${SCHEMA_NAME}/schema/`, BASE_URL).toString();
    await page.goto(schemaUrl);

    const samplingRow = page.getByRole("row").filter({ has: page.getByText("sampling") }).first();
    await expect(samplingRow).toContainText("INTERNAL");

    const sequencingRow = page.getByRole("row").filter({ has: page.getByText("sequencing") }).first();
    await expect(sequencingRow).toContainText("INTERNAL");
  });

  test("should display PROFILE column type for experiment_type", async ({
    page,
  }) => {
    const schemaUrl = new URL(`/${SCHEMA_NAME}/schema/`, BASE_URL).toString();
    await page.goto(schemaUrl);

    const experimentsTableRow = page.getByRole("row").filter({ has: page.getByText("Experiments") }).first();
    await experimentsTableRow.click();

    const profileColumn = page.getByRole("cell", { name: "experiment_type" }).first();
    await expect(profileColumn).toBeVisible();

    const profileTypeCell = page.getByRole("row").filter({
      has: page.getByText("experiment_type"),
    }).getByText("EXTENSION");
    await expect(profileTypeCell).toBeVisible();
  });

  test("should display inheritance relationships in diagram view", async ({
    page,
  }) => {
    const schemaUrl = new URL(`/${SCHEMA_NAME}/schema/`, BASE_URL).toString();
    await page.goto(schemaUrl);

    const showDiagramButton = page.getByRole("button", { name: /Show Diagram/i });
    await expect(showDiagramButton).toBeVisible();
    await showDiagramButton.click();

    const diagramSection = page.locator("#molgenis_diagram_anchor");
    await expect(diagramSection).toBeVisible();

    const svgElement = page.locator("svg").first();
    await expect(svgElement).toBeVisible();
  });
});
