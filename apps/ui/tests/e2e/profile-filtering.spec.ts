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

async function setActiveProfiles(
  request: any,
  profiles: string[]
): Promise<void> {
  const profilesArg = profiles.map((p) => `"${p}"`).join(", ");
  const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
    data: {
      query: `mutation { change(activeProfiles: [${profilesArg}]) { message } }`,
    },
  });

  const result = await response.json();
  if (result.errors) {
    throw new Error(
      `Failed to set active profiles: ${JSON.stringify(result.errors)}`
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
  test.describe.configure({ mode: "serial" });

  test.beforeAll(async ({ request }) => {
    await signin(request);
    await deleteTestSchema(request);
    await createTestSchema(request);
    await createTablesWithProfiles(request);
    await setActiveProfiles(request, ["wgs"]);
  });

  test.afterAll(async ({ request }) => {
    await signin(request);
    await deleteTestSchema(request);
  });

  test.beforeEach(async ({ request }) => {
    await signin(request);
  });

  test("should filter tables via API with applyProfileFilter", async ({
    request,
  }) => {
    const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `{ _schema(applyProfileFilter: true) { tables { name profiles } } }`,
      },
    });

    const result = await response.json();
    expect(result.errors).toBeUndefined();

    const tables = result.data._schema.tables;
    const tableNames = tables.map((t: any) => t.name);

    expect(tableNames).toContain("Samples");
    expect(tableNames).toContain("WGS");
    expect(tableNames).not.toContain("Imaging");
  });

  test("should filter columns via API with applyProfileFilter", async ({
    request,
  }) => {
    const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `{ _schema(applyProfileFilter: true) { tables { name columns { name profiles } } } }`,
      },
    });

    const result = await response.json();
    expect(result.errors).toBeUndefined();

    const tables = result.data._schema.tables;
    const samplesTable = tables.find((t: any) => t.name === "Samples");
    const samplesColumnNames = samplesTable.columns.map((c: any) => c.name);

    expect(samplesColumnNames).toContain("id");
    expect(samplesColumnNames).toContain("name");
    expect(samplesColumnNames).toContain("wgs_field");
  });

  test("should expose activeProfiles in schema metadata", async ({
    request,
  }) => {
    const response = await request.post(`/${SCHEMA_NAME}/api/graphql`, {
      data: {
        query: `{ _schema { activeProfiles } }`,
      },
    });

    const result = await response.json();
    expect(result.errors).toBeUndefined();

    const activeProfiles = result.data._schema.activeProfiles;
    expect(activeProfiles).toEqual(["wgs"]);
  });

  test("should only show profile-matching tables in UI listing", async ({
    page,
  }) => {
    await page.goto(`${route}${SCHEMA_NAME}`);
    await page.getByRole("button", { name: "Signin" }).click();
    await page.getByRole("textbox", { name: "Username" }).fill("admin");
    await page.getByRole("textbox", { name: "Username" }).press("Tab");
    await page.getByRole("textbox", { name: "Password" }).fill("admin");
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
    await page.goto(`${route}${SCHEMA_NAME}`);
    await page.waitForLoadState("networkidle");

    await expect(page.getByText("Samples")).toBeVisible({ timeout: 10000 });
    await expect(page.getByText("WGS")).toBeVisible();
    await expect(page.getByText("Imaging")).not.toBeVisible();
  });

  test("should show all columns in form when no profiles active", async ({
    page,
    request,
  }) => {
    await signin(request);
    await insertSampleRecord(request);

    await page.goto(`${route}`);
    await page.getByRole("button", { name: "Signin" }).click();
    await page.getByRole("textbox", { name: "Username" }).fill("admin");
    await page.getByRole("textbox", { name: "Username" }).press("Tab");
    await page.getByRole("textbox", { name: "Password" }).fill("admin");
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(page.getByRole("button", { name: "Account" })).toBeVisible();
    await page.goto(`${route}${SCHEMA_NAME}/Samples`);
    await page.waitForLoadState("networkidle");
    const sampleLink = page
      .getByRole("cell", { name: "test-record-1" })
      .first();
    await expect(sampleLink).toBeVisible({ timeout: 10000 });
    await sampleLink.click();

    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      "Samples"
    );

    await page.waitForLoadState("networkidle");
    await expect(page.getByText("name", { exact: true })).toBeVisible();
    await expect(page.getByText("wgs_field")).toBeVisible();

    await deleteSampleRecord(request);
  });
});
