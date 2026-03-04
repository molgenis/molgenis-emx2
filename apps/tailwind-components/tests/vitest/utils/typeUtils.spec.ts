import { describe, expect, it, vi } from "vitest";
import {
  getInitialFormValues,
  getOntologyArrayValues,
} from "../../../app/utils/typeUtils";

describe("getInitialFormValues", () => {
  it("should return initial form values based on metadata", () => {
    const metadata = {
      columns: [
        { id: "col1", defaultValue: "default1", columnType: "STRING" },
        { id: "col2", defaultValue: "=1 + 1", columnType: "INT" },
        { id: "col3", defaultValue: "TRUE", columnType: "BOOL" },
        { id: "col4", columnType: "STRING" },
      ],
    } as any;
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
    } as any;
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
    } as any;

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
    } as any;

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
