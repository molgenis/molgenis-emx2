import { describe, expect, test } from "vitest";
import {
  getDataTables,
  SELECT_OPTIONS,
  MODIFY_OPTIONS,
  type ITableInfo,
} from "../../../app/util/roleUtils";

describe("roleUtils constants", () => {
  test("SELECT_OPTIONS has 6 entries", () => {
    expect(SELECT_OPTIONS).toHaveLength(6);
    expect(SELECT_OPTIONS).toEqual([
      "EXISTS",
      "RANGE",
      "AGGREGATOR",
      "COUNT",
      "TABLE",
      "ROW",
    ]);
  });

  test("MODIFY_OPTIONS has 2 entries", () => {
    expect(MODIFY_OPTIONS).toHaveLength(2);
    expect(MODIFY_OPTIONS).toEqual(["TABLE", "ROW"]);
  });
});

describe("getDataTables", () => {
  test("filters out non-DATA tables", () => {
    const tables: ITableInfo[] = [
      { id: "Patients", label: "Patients", tableType: "DATA" },
      { id: "Diseases", label: "Diseases", tableType: "ONTOLOGIES" },
      { id: "Samples", label: "Samples", tableType: "DATA" },
      { id: "Terms", label: "Terms", tableType: "SETTINGS" },
    ];

    const result = getDataTables(tables);

    expect(result).toHaveLength(2);
    expect(result.map((t) => t.id)).toEqual(["Patients", "Samples"]);
  });

  test("sorts by label alphabetically", () => {
    const tables: ITableInfo[] = [
      { id: "Zebra", label: "Zebra Study", tableType: "DATA" },
      { id: "Alpha", label: "Alpha Project", tableType: "DATA" },
      { id: "Beta", label: "Beta Test", tableType: "DATA" },
    ];

    const result = getDataTables(tables);

    expect(result.map((t) => t.label)).toEqual([
      "Alpha Project",
      "Beta Test",
      "Zebra Study",
    ]);
  });

  test("returns empty array for empty input", () => {
    const result = getDataTables([]);
    expect(result).toEqual([]);
  });

  test("returns empty array when no DATA tables exist", () => {
    const tables: ITableInfo[] = [
      { id: "Diseases", label: "Diseases", tableType: "ONTOLOGIES" },
      { id: "Terms", label: "Terms", tableType: "SETTINGS" },
    ];

    const result = getDataTables(tables);

    expect(result).toEqual([]);
  });

  test("preserves all properties of filtered tables", () => {
    const tables: ITableInfo[] = [
      { id: "Patients", label: "Patients", tableType: "DATA" },
    ];

    const result = getDataTables(tables);

    expect(result[0]).toEqual({
      id: "Patients",
      label: "Patients",
      tableType: "DATA",
    });
  });
});
