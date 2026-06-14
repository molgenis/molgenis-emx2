import { describe, it, expect } from "vitest";
import type { ISchemaMetaData, ITableMetaData } from "../src/types";
import { expandModuleColumns, activeModules } from "../src/moduleColumns";

const rootColumns = [
  { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
  { id: "name", label: "Name", columnType: "STRING" as const },
  {
    id: "measurements",
    label: "Measurements",
    columnType: "MODULE_ARRAY" as const,
    values: ["BodyMeasurements", "BloodPressure"],
  },
  {
    id: "imaging",
    label: "Imaging",
    columnType: "MODULE_ARRAY" as const,
    values: ["MRI"],
  },
];

const bodyMeasurementsTable: ITableMetaData = {
  id: "BodyMeasurements",
  name: "BodyMeasurements",
  schemaId: "test",
  label: "Body Measurements",
  tableType: "MODULE",
  columns: [
    { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
    { id: "name", label: "Name", columnType: "STRING" as const },
    { id: "height", label: "Height", columnType: "DECIMAL" as const },
    { id: "weight", label: "Weight", columnType: "DECIMAL" as const },
  ],
};

const bloodPressureTable: ITableMetaData = {
  id: "BloodPressure",
  name: "BloodPressure",
  schemaId: "test",
  label: "Blood Pressure",
  tableType: "MODULE",
  columns: [
    { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
    { id: "name", label: "Name", columnType: "STRING" as const },
    { id: "systolic", label: "Systolic", columnType: "INT" as const },
    { id: "diastolic", label: "Diastolic", columnType: "INT" as const },
  ],
};

const mriTable: ITableMetaData = {
  id: "MRI",
  name: "MRI",
  schemaId: "test",
  label: "MRI",
  tableType: "MODULE",
  columns: [
    { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
    { id: "name", label: "Name", columnType: "STRING" as const },
    { id: "scanDate", label: "Scan Date", columnType: "DATE" as const },
  ],
};

const rootTable: ITableMetaData = {
  id: "Patient",
  name: "Patient",
  schemaId: "test",
  label: "Patient",
  tableType: "DATA",
  columns: rootColumns,
};

const schema: ISchemaMetaData = {
  id: "test",
  label: "Test Schema",
  tables: [rootTable, bodyMeasurementsTable, bloodPressureTable, mriTable],
};

describe("expandModuleColumns", () => {
  it("returns empty array for a root with no MODULE_ARRAY columns", () => {
    const noModuleRoot: ITableMetaData = {
      id: "Simple",
      name: "Simple",
      schemaId: "test",
      label: "Simple",
      tableType: "DATA",
      columns: [
        { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
      ],
    };
    expect(expandModuleColumns(noModuleRoot, schema)).toEqual([]);
  });

  it("returns groups ordered by axis column then values order", () => {
    const groups = expandModuleColumns(rootTable, schema);

    expect(groups.map((g) => g.moduleName)).toEqual([
      "BodyMeasurements",
      "BloodPressure",
      "MRI",
    ]);
  });

  it("local columns exclude root table columns (matched by id)", () => {
    const groups = expandModuleColumns(rootTable, schema);

    const bodyGroup = groups.find((g) => g.moduleName === "BodyMeasurements");
    expect(bodyGroup).toBeDefined();
    const columnIds = bodyGroup!.columns.map((col) => col.id);
    expect(columnIds).toContain("height");
    expect(columnIds).toContain("weight");
    expect(columnIds).not.toContain("id");
    expect(columnIds).not.toContain("name");
  });

  it("flat module with extra cols: local cols exclude root cols (id, name)", () => {
    const extendedModuleTable: ITableMetaData = {
      id: "DetailedMRI",
      name: "DetailedMRI",
      schemaId: "test",
      label: "Detailed MRI",
      tableType: "MODULE",
      columns: [
        { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
        { id: "name", label: "Name", columnType: "STRING" as const },
        { id: "scanDate", label: "Scan Date", columnType: "DATE" as const },
        {
          id: "resolution",
          label: "Resolution",
          columnType: "DECIMAL" as const,
        },
      ],
    };

    const rootWithExtended: ITableMetaData = {
      ...rootTable,
      columns: [
        { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
        { id: "name", label: "Name", columnType: "STRING" as const },
        {
          id: "imaging",
          label: "Imaging",
          columnType: "MODULE_ARRAY" as const,
          values: ["DetailedMRI"],
        },
      ],
    };

    const extendedSchema: ISchemaMetaData = {
      ...schema,
      tables: [...schema.tables, extendedModuleTable],
    };

    const groups = expandModuleColumns(rootWithExtended, extendedSchema);
    const group = groups.find((g) => g.moduleName === "DetailedMRI");
    expect(group).toBeDefined();
    const columnIds = group!.columns.map((col) => col.id);
    expect(columnIds).toContain("scanDate");
    expect(columnIds).toContain("resolution");
    expect(columnIds).not.toContain("id");
    expect(columnIds).not.toContain("name");
  });

  it("module-extends-module chain: child group includes parent-local cols but excludes root cols", () => {
    const rootBase: ITableMetaData = {
      id: "Subject",
      name: "Subject",
      schemaId: "test",
      label: "Subject",
      tableType: "DATA",
      columns: [
        {
          id: "subjectId",
          label: "Subject ID",
          columnType: "STRING" as const,
          key: 1,
        },
        { id: "subjectName", label: "Name", columnType: "STRING" as const },
        {
          id: "exams",
          label: "Exams",
          columnType: "MODULE_ARRAY" as const,
          values: ["AdvancedCockayne"],
        },
      ],
    };

    const moduleA: ITableMetaData = {
      id: "BasicCockayne",
      name: "BasicCockayne",
      schemaId: "test",
      label: "Basic Cockayne",
      tableType: "MODULE",
      inheritNames: ["Subject"],
      columns: [
        {
          id: "subjectId",
          label: "Subject ID",
          columnType: "STRING" as const,
          key: 1,
        },
        { id: "subjectName", label: "Name", columnType: "STRING" as const },
        { id: "severity", label: "Severity", columnType: "STRING" as const },
        {
          id: "ageAtDiagnosis",
          label: "Age at Diagnosis",
          columnType: "INT" as const,
        },
      ],
    };

    const moduleB: ITableMetaData = {
      id: "AdvancedCockayne",
      name: "AdvancedCockayne",
      schemaId: "test",
      label: "Advanced Cockayne",
      tableType: "MODULE",
      inheritNames: ["BasicCockayne"],
      columns: [
        {
          id: "subjectId",
          label: "Subject ID",
          columnType: "STRING" as const,
          key: 1,
        },
        { id: "subjectName", label: "Name", columnType: "STRING" as const },
        { id: "severity", label: "Severity", columnType: "STRING" as const },
        {
          id: "ageAtDiagnosis",
          label: "Age at Diagnosis",
          columnType: "INT" as const,
        },
        { id: "csaScore", label: "CSA Score", columnType: "DECIMAL" as const },
        {
          id: "uvSensitivity",
          label: "UV Sensitivity",
          columnType: "BOOL" as const,
        },
      ],
    };

    const chainSchema: ISchemaMetaData = {
      id: "test",
      label: "Chain Schema",
      tables: [rootBase, moduleA, moduleB],
    };

    const groups = expandModuleColumns(rootBase, chainSchema);
    const group = groups.find((g) => g.moduleName === "AdvancedCockayne");
    expect(group).toBeDefined();
    const columnIds = group!.columns.map((col) => col.id);

    expect(columnIds).toContain("severity");
    expect(columnIds).toContain("ageAtDiagnosis");
    expect(columnIds).toContain("csaScore");
    expect(columnIds).toContain("uvSensitivity");

    expect(columnIds).not.toContain("subjectId");
    expect(columnIds).not.toContain("subjectName");
  });

  it("handles two orthogonal axes producing separate groups", () => {
    const groups = expandModuleColumns(rootTable, schema);
    const moduleNames = groups.map((g) => g.moduleName);
    expect(moduleNames).toContain("BodyMeasurements");
    expect(moduleNames).toContain("BloodPressure");
    expect(moduleNames).toContain("MRI");
    expect(moduleNames).toHaveLength(3);
  });

  it("ignores unknown module names in values (no crash)", () => {
    const rootWithUnknown: ITableMetaData = {
      ...rootTable,
      columns: [
        {
          id: "modules",
          label: "Modules",
          columnType: "MODULE_ARRAY" as const,
          values: ["BodyMeasurements", "NonExistentModule"],
        },
      ],
    };
    const groups = expandModuleColumns(rootWithUnknown, schema);
    const moduleNames = groups.map((g) => g.moduleName);
    expect(moduleNames).toContain("BodyMeasurements");
    expect(moduleNames).not.toContain("NonExistentModule");
    expect(moduleNames).toHaveLength(1);
  });
});

describe("activeModules", () => {
  it("returns empty set for row with no discriminator values", () => {
    const row = { id: "p1", name: "Alice" };
    const result = activeModules(row, rootTable);
    expect(result.size).toBe(0);
  });

  it("returns module names from a single active discriminator", () => {
    const row = {
      id: "p1",
      name: "Alice",
      measurements: ["BodyMeasurements"],
      imaging: [],
    };
    const result = activeModules(row, rootTable);
    expect(result.has("BodyMeasurements")).toBe(true);
    expect(result.has("BloodPressure")).toBe(false);
    expect(result.has("MRI")).toBe(false);
  });

  it("returns union across multiple axes when both have active modules", () => {
    const row = {
      id: "p1",
      name: "Alice",
      measurements: ["BodyMeasurements", "BloodPressure"],
      imaging: ["MRI"],
    };
    const result = activeModules(row, rootTable);
    expect(result.has("BodyMeasurements")).toBe(true);
    expect(result.has("BloodPressure")).toBe(true);
    expect(result.has("MRI")).toBe(true);
  });

  it("ignores non-array discriminator values (no crash)", () => {
    const row = {
      id: "p1",
      measurements: null,
      imaging: undefined,
    };
    const result = activeModules(row, rootTable);
    expect(result.size).toBe(0);
  });

  it("ignores unknown module name strings in discriminator (no crash)", () => {
    const row = {
      id: "p1",
      measurements: ["NonExistentModule"],
    };
    const result = activeModules(row, rootTable);
    expect(result.has("NonExistentModule")).toBe(true);
  });
});
