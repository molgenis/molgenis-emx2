import { describe, expect, it, vi } from "vitest";
import {
  assertBooleanValue,
  assertFileValue,
  assertListValue,
  assertNumberValue,
  assertRefColumn,
  assertRefColumnValue,
  assertRowValue,
  assertStringValue,
  assertTableValue,
  getInitialFormValues,
  getOntologyArrayValues,
  toRefColumn,
  toRefColumnValue,
} from "../../../app/utils/typeUtils";
import type {
  columnValue,
  IColumn,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";

describe("getInitialFormValues", () => {
  it("should return initial form values based on metadata", () => {
    const metadata = {
      columns: [
        { id: "col1", defaultValue: "default1", columnType: "STRING" },
        { id: "col2", defaultValue: "=1 + 1", columnType: "INT" },
        { id: "col3", defaultValue: "TRUE", columnType: "BOOL" },
        { id: "col4", columnType: "STRING" },
      ],
    } as ITableMetaData;
    const initialValues = getInitialFormValues(metadata);
    const expectedValue = {
      col1: "default1",
      col2: 2,
      col3: true,
    };
    expect(initialValues).toEqual(expectedValue);
  });

  it("should return empty object when no default values are set", () => {
    const metadata = {
      columns: [
        { id: "col1", columnType: "STRING" },
        { id: "col2", columnType: "INT" },
        { id: "col3", columnType: "BOOL" },
      ],
    } as ITableMetaData;
    const initialValues = getInitialFormValues(metadata);
    const expectedValue = {};
    expect(initialValues).toEqual(expectedValue);
  });

  it("should handle expression errors gracefully", () => {
    const consoleMock = vi
      .spyOn(console, "error")
      .mockImplementation(() => undefined);

    const metadata = {
      columns: [
        { id: "col1", defaultValue: "=invalidExpression()", columnType: "INT" },
      ],
    } as ITableMetaData;

    const initialValues = getInitialFormValues(metadata);

    const expectedValue = {};
    const invalidError =
      "Default value expression failed for column col1: ReferenceError: invalidExpression is not defined";
    expect(initialValues).toEqual(expectedValue);
    expect(consoleMock).toHaveBeenCalledOnce();
    expect(consoleMock).toHaveBeenLastCalledWith(invalidError);
  });

  it("should be able to resolve different bools", () => {
    const metadata = {
      columns: [
        { id: "col1", defaultValue: "=true", columnType: "BOOL" },
        { id: "col2", defaultValue: "true", columnType: "BOOL" },
        { id: "col3", defaultValue: "TRUE", columnType: "BOOL" },
        { id: "col4", defaultValue: "=false", columnType: "BOOL" },
        { id: "col5", defaultValue: "false", columnType: "BOOL" },
        { id: "col6", defaultValue: "FALSE", columnType: "BOOL" },
        { id: "col7", columnType: "BOOL" },
      ],
    } as ITableMetaData;

    const initialValues = getInitialFormValues(metadata);
    const expectedValue = {
      col1: true,
      col2: true,
      col3: true,
      col4: false,
      col5: false,
      col6: false,
      col7: undefined,
    };

    expect(initialValues).toEqual(expectedValue);
  });
});

describe("getOntologyArrayValues", () => {
  it("should return array of names from ontology values", () => {
    const values = [
      { name: "Name1", id: "id1" },
      { name: "Name2", id: "id2" },
      null,
      { name: "Name3", id: "id3" },
    ];
    const result = getOntologyArrayValues(values);
    const expectedResult = ["Name1", "Name2", "Name3"];
    expect(result).toEqual(expectedResult);
  });

  it("should return empty array when input is not an array", () => {
    const values = null;
    const result = getOntologyArrayValues(values);
    const expectedResult: string[] = [];
    expect(result).toEqual(expectedResult);
  });
});

describe("assertStringValue", () => {
  it("returns valid string-like values", () => {
    expect(assertStringValue("value")).toBe("value");
    expect(assertStringValue(null)).toBeNull();
    expect(assertStringValue(undefined)).toBeUndefined();
  });

  it("throws for non-string values", () => {
    expect(() => assertStringValue(1 as columnValue)).toThrow(
      "Expected a string value, but got number"
    );
  });
});

describe("assertNumberValue", () => {
  it("returns numbers and numeric strings", () => {
    expect(assertNumberValue(12)).toBe(12);
    expect(assertNumberValue("12")).toBe(12);
    expect(assertNumberValue(null)).toBeNull();
    expect(assertNumberValue(undefined)).toBeUndefined();
  });

  it("throws for invalid numeric values", () => {
    expect(() => assertNumberValue("abc" as columnValue)).toThrow(
      "Expected a number value, but got string"
    );
    expect(() => assertNumberValue(true as columnValue)).toThrow(
      "Expected a number value, but got boolean"
    );
  });
});

describe("assertBooleanValue", () => {
  it("returns valid boolean-like values", () => {
    expect(assertBooleanValue(true)).toBe(true);
    expect(assertBooleanValue(false)).toBe(false);
    expect(assertBooleanValue(null)).toBeNull();
    expect(assertBooleanValue(undefined)).toBeUndefined();
  });

  it("throws for non-boolean values", () => {
    expect(() => assertBooleanValue("true" as columnValue)).toThrow(
      "Expected a boolean value, but got string"
    );
  });
});

describe("assertRowValue", () => {
  it("returns plain object rows", () => {
    const row = { id: "1", name: "Tweety" };
    expect(assertRowValue(row)).toEqual(row);
  });

  it("throws for non-row values", () => {
    expect(() => assertRowValue(null)).toThrow(
      "Expected an object value, but got object"
    );
    expect(() => assertRowValue([] as columnValue)).toThrow(
      "Expected an object value, but got object"
    );
    expect(() => assertRowValue("row" as columnValue)).toThrow(
      "Expected an object value, but got string"
    );
  });
});

describe("assertTableValue", () => {
  it("returns arrays of rows and nullable values", () => {
    const rows = [{ id: "1" }, { id: "2" }];
    expect(assertTableValue(rows)).toEqual(rows);
    expect(assertTableValue(null)).toBeNull();
    expect(assertTableValue(undefined)).toBeUndefined();
  });

  it("throws for invalid table values", () => {
    expect(() => assertTableValue("rows" as columnValue)).toThrow(
      "Expected an array value, but got string"
    );
    expect(() => assertTableValue([1] as columnValue)).toThrow(
      "Expected an object value, but got number"
    );
  });
});

describe("assertFileValue", () => {
  it("returns valid file objects and treats empty objects as undefined", () => {
    const file = { filename: "report.csv", size: 42 };
    expect(assertFileValue(file as columnValue)).toEqual(file);
    expect(assertFileValue({} as columnValue)).toBeUndefined();
  });

  it("throws for invalid file values", () => {
    expect(() => assertFileValue("file" as columnValue)).toThrow(
      "Expected an object value, but got string"
    );
    expect(() => assertFileValue({ filename: 42 } as columnValue)).toThrow(
      "Expected an object with a string 'filename' property"
    );
  });
});

describe("assertListValue", () => {
  it("returns lists and nullable values", () => {
    const list = ["a", 1, { id: "2" }];
    expect(assertListValue(list as columnValue)).toEqual(list);
    expect(assertListValue(null)).toBeNull();
    expect(assertListValue(undefined)).toBeUndefined();
  });

  it("throws for non-list values", () => {
    expect(() => assertListValue("list" as columnValue)).toThrow(
      "Expected an array value, but got string"
    );
  });
});

describe("reference helpers", () => {
  const refColumn = {
    id: "bird",
    label: "Bird",
    columnType: "REF",
    refTableId: "birds",
    refSchemaId: "petstore",
  } as IColumn;

  const rowValue = { id: "bird-1" };

  it("accepts and converts valid reference columns", () => {
    expect(() => assertRefColumn(refColumn)).not.toThrow();
    expect(toRefColumn(refColumn)).toBe(refColumn);
  });

  it("rejects invalid reference columns", () => {
    expect(() =>
      assertRefColumn({ id: "bird", columnType: "REF" } as IColumn)
    ).toThrow("Column is not a valid reference column");
  });

  it("accepts and converts valid reference values", () => {
    expect(() => assertRefColumnValue(rowValue)).not.toThrow();
    expect(toRefColumnValue(rowValue)).toBe(rowValue);
  });

  it("rejects invalid reference values", () => {
    expect(() => assertRefColumnValue(null)).toThrow(
      "Value is not a valid reference column value"
    );
    expect(() => assertRefColumnValue([] as columnValue)).toThrow(
      "Value is not a valid reference column value"
    );
  });
});
