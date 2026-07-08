import { describe, it, expect } from "vitest";
import type { ISchemaMetaData, ITableMetaData } from "../src/types";
import {
  expandModuleColumns,
  activeModules,
  isModuleColumn,
  omitInactiveModuleValues,
} from "../src/moduleColumns";

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

describe("isModuleColumn", () => {
  it("returns false for a root/local column (table === tableName)", () => {
    expect(
      isModuleColumn(
        { columnType: "STRING", id: "id", label: "ID", table: "Subject" },
        "Subject"
      )
    ).toBe(false);
  });

  it("returns false for an inherited column (inherited: true)", () => {
    expect(
      isModuleColumn(
        {
          columnType: "STRING",
          id: "id",
          label: "ID",
          table: "ModX",
          inherited: true,
        },
        "Subject"
      )
    ).toBe(false);
  });

  it("returns false when table property is absent", () => {
    expect(
      isModuleColumn({ columnType: "STRING", id: "id", label: "ID" }, "Subject")
    ).toBe(false);
  });

  it("returns true for a foreign module column (different table, not inherited)", () => {
    expect(
      isModuleColumn(
        {
          columnType: "STRING",
          id: "modxCol",
          label: "ModX col",
          table: "ModX",
          inherited: false,
        },
        "Subject"
      )
    ).toBe(true);
  });
});

describe("omitInactiveModuleValues", () => {
  const subjectTable: ITableMetaData = {
    id: "Subject",
    name: "Subject",
    schemaId: "test",
    label: "Subject",
    tableType: "DATA",
    columns: [
      {
        columnType: "STRING",
        id: "id",
        label: "ID",
        table: "Subject",
        inherited: false,
      },
      {
        columnType: "MODULE_ARRAY",
        id: "moduleAxis",
        label: "Module axis",
        table: "Subject",
        inherited: false,
      },
      {
        columnType: "STRING",
        id: "rootCol",
        label: "Root column",
        table: "Subject",
        inherited: false,
      },
      {
        columnType: "STRING",
        id: "modxCol",
        label: "ModX column",
        table: "ModX",
        inherited: false,
      },
    ],
  };

  it("drops values for inactive module columns", () => {
    const values = {
      id: "row1",
      moduleAxis: [],
      rootCol: "rootVal",
      modxCol: "modxVal",
    };
    const result = omitInactiveModuleValues(values, subjectTable, new Set());
    expect(Object.keys(result)).toContain("id");
    expect(Object.keys(result)).toContain("rootCol");
    expect(Object.keys(result)).toContain("moduleAxis");
    expect(Object.keys(result)).not.toContain("modxCol");
  });

  it("keeps values for active module columns", () => {
    const values = {
      id: "row1",
      moduleAxis: ["ModX"],
      rootCol: "rootVal",
      modxCol: "modxVal",
    };
    const result = omitInactiveModuleValues(
      values,
      subjectTable,
      new Set(["ModX"])
    );
    expect(Object.keys(result)).toContain("modxCol");
    expect(result["modxCol"]).toBe("modxVal");
  });

  it("always keeps root columns regardless of active modules", () => {
    const values = { id: "row1", rootCol: "rootVal" };
    const result = omitInactiveModuleValues(values, subjectTable, new Set());
    expect(result["id"]).toBe("row1");
    expect(result["rootCol"]).toBe("rootVal");
  });

  it("keeps keys with no matching column entry (e.g. mg_draft)", () => {
    const values = { mg_draft: true, modxCol: "modxVal" };
    const result = omitInactiveModuleValues(values, subjectTable, new Set());
    expect(result["mg_draft"]).toBe(true);
    expect(Object.keys(result)).not.toContain("modxCol");
  });

  const experimentTable: ITableMetaData = {
    id: "Experiment",
    name: "Experiment",
    schemaId: "test",
    label: "Experiment",
    tableType: "DATA",
    columns: [
      {
        columnType: "STRING",
        id: "id",
        label: "ID",
        table: "Experiment",
        inherited: false,
      },
      {
        columnType: "MODULE",
        id: "experimentType",
        label: "Experiment Type",
        values: ["RNA", "DNA"],
        table: "Experiment",
        inherited: false,
      },
      {
        columnType: "STRING",
        id: "readCount",
        label: "Read count",
        table: "RNA",
        inherited: false,
      },
    ],
  };

  it("drops a scalar-module content col when its module is inactive (using activeModules from a scalar MODULE row)", () => {
    const row = { id: "e1", experimentType: "DNA", readCount: 42 };
    const active = activeModules(row, experimentTable);
    const result = omitInactiveModuleValues(row, experimentTable, active);
    expect(Object.keys(result)).not.toContain("readCount");
  });

  it("keeps a scalar-module content col when its module is active (using activeModules from a scalar MODULE row)", () => {
    const row = { id: "e1", experimentType: "RNA", readCount: 42 };
    const active = activeModules(row, experimentTable);
    const result = omitInactiveModuleValues(row, experimentTable, active);
    expect(Object.keys(result)).toContain("readCount");
    expect(result["readCount"]).toBe(42);
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

  const rootWithScalarModule: ITableMetaData = {
    id: "Experiment",
    name: "Experiment",
    schemaId: "test",
    label: "Experiment",
    tableType: "DATA",
    columns: [
      { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
      {
        id: "experimentType",
        label: "Experiment Type",
        columnType: "MODULE" as const,
        values: ["RNA", "DNA"],
      },
    ],
  };

  it("collects a scalar MODULE single value when set", () => {
    const row = { id: "e1", experimentType: "RNA" };
    const result = activeModules(row, rootWithScalarModule);
    expect(result.has("RNA")).toBe(true);
    expect(result.has("DNA")).toBe(false);
    expect(result.size).toBe(1);
  });

  it("does not collect a scalar MODULE value when unset (null/undefined/empty string)", () => {
    expect(
      activeModules({ id: "e1", experimentType: null }, rootWithScalarModule)
        .size
    ).toBe(0);
    expect(
      activeModules(
        { id: "e1", experimentType: undefined },
        rootWithScalarModule
      ).size
    ).toBe(0);
    expect(
      activeModules({ id: "e1", experimentType: "" }, rootWithScalarModule).size
    ).toBe(0);
  });

  const rootWithBothAxisTypes: ITableMetaData = {
    id: "Subject",
    name: "Subject",
    schemaId: "test",
    label: "Subject",
    tableType: "DATA",
    columns: [
      { id: "id", label: "ID", columnType: "STRING" as const, key: 1 },
      {
        id: "measurements",
        label: "Measurements",
        columnType: "MODULE_ARRAY" as const,
        values: ["BodyMeasurements", "BloodPressure"],
      },
      {
        id: "experimentType",
        label: "Experiment Type",
        columnType: "MODULE" as const,
        values: ["RNA", "DNA"],
      },
    ],
  };

  it("unions a scalar MODULE axis and a MODULE_ARRAY axis coexisting on one table", () => {
    const row = {
      id: "s1",
      measurements: ["BodyMeasurements"],
      experimentType: "RNA",
    };
    const result = activeModules(row, rootWithBothAxisTypes);
    expect(result.has("BodyMeasurements")).toBe(true);
    expect(result.has("RNA")).toBe(true);
    expect(result.size).toBe(2);
  });
});
