import { describe, it, expect } from "vitest";
import type { ISchemaMetaData, ITableMetaData } from "../src/types";
import {
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

const rootTable: ITableMetaData = {
  id: "Patient",
  name: "Patient",
  schemaId: "test",
  label: "Patient",
  tableType: "DATA",
  columns: rootColumns,
};

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

describe("activeModules ancestor-module activation (module chain)", () => {
  const chainSchema: ISchemaMetaData = {
    id: "test",
    label: "test",
    tables: [
      {
        id: "Root",
        name: "Root",
        schemaId: "test",
        label: "Root",
        tableType: "DATA",
        columns: [
          { id: "id", label: "ID", columnType: "STRING", key: 1 },
          {
            id: "modules",
            label: "Modules",
            columnType: "MODULE_ARRAY",
            values: ["M1", "M2"],
            table: "Root",
          },
          { id: "m1Col", label: "M1 col", columnType: "STRING", table: "M1" },
          { id: "m2Col", label: "M2 col", columnType: "STRING", table: "M2" },
        ],
      },
      {
        id: "M1",
        name: "M1",
        schemaId: "test",
        label: "M1",
        tableType: "MODULE",
        inheritNames: ["Root"],
        columns: [],
      },
      {
        id: "M2",
        name: "M2",
        schemaId: "test",
        label: "M2",
        tableType: "MODULE",
        inheritNames: ["M1"],
        columns: [],
      },
    ],
  };
  const chainRoot = chainSchema.tables[0]!;

  it("activates ancestor module M1 when leaf module M2 is active (Root<-M1<-M2)", () => {
    const row = { id: "r1", modules: ["M2"] };
    const result = activeModules(row, chainRoot, chainSchema);
    expect(result.has("M2")).toBe(true);
    expect(result.has("M1")).toBe(true);
  });

  it("does not activate the DATA root as a module ancestor", () => {
    const row = { id: "r1", modules: ["M2"] };
    const result = activeModules(row, chainRoot, chainSchema);
    expect(result.has("Root")).toBe(false);
  });

  it("activates ancestor of an active scalar MODULE leaf", () => {
    const scalarSchema: ISchemaMetaData = {
      id: "test",
      label: "test",
      tables: [
        {
          id: "Host",
          name: "Host",
          schemaId: "test",
          label: "Host",
          tableType: "DATA",
          columns: [
            { id: "id", label: "ID", columnType: "STRING", key: 1 },
            {
              id: "assay",
              label: "Assay",
              columnType: "MODULE",
              values: ["ModA", "DeepA"],
              table: "Host",
            },
          ],
        },
        {
          id: "ModA",
          name: "ModA",
          schemaId: "test",
          label: "ModA",
          tableType: "MODULE",
          inheritNames: ["Host"],
          columns: [],
        },
        {
          id: "DeepA",
          name: "DeepA",
          schemaId: "test",
          label: "DeepA",
          tableType: "MODULE",
          inheritNames: ["ModA"],
          columns: [],
        },
      ],
    };
    const row = { id: "e1", assay: "DeepA" };
    const result = activeModules(row, scalarSchema.tables[0]!, scalarSchema);
    expect(result.has("DeepA")).toBe(true);
    expect(result.has("ModA")).toBe(true);
  });
});
