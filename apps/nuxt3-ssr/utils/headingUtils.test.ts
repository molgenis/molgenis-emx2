import { describe, it, expect } from "vitest";

import { toHeadings, toSectionsMap } from "./headingUtils";
import { ITableMetaData } from "meta-data-utils";

describe("toHeadings", () => {
  it("should return only the heading columns", () => {
    const tableMetaData: ITableMetaData = {
      id: "test",
      label: "test",
      tableType: "test",
      schemaId: "test",
      columns: [
        { id: "test", columnType: "HEADING", label: "test" },
        { id: "test2", columnType: "TEXT", label: "test2" },
      ],
    };

    expect(toHeadings(tableMetaData)).toEqual([
      { id: "test", columnType: "HEADING", label: "test" },
    ]);
  });

  it("should return empty list in case of no headings", () => {
    const tableMetaData: ITableMetaData = {
      id: "test",
      label: "test",
      tableType: "test",
      schemaId: "test",
      columns: [],
    };

    expect(toHeadings(tableMetaData)).toEqual([]);
  });
});

describe("toSectionsMap", () => {
  const tableMetaData: ITableMetaData = {
    id: "test",
    label: "test",
    tableType: "test",
    schemaId: "test",
    columns: [],
  };

  it("should always return the default section", () => {
    let emptyColumns = structuredClone(tableMetaData);
    emptyColumns.columns = [];
    expect(toSectionsMap(emptyColumns)).toEqual({ DEFAULT_SECTION: [] });
  });

  it("place each column in its section", () => {
    let withSections = structuredClone(tableMetaData);
    withSections.columns = [
      { id: "test", columnType: "STRING", label: "test" },
      { id: "test2", columnType: "TEXT", label: "test2" },
      { id: "test3", columnType: "HEADING", label: "section1" },
      { id: "test4", columnType: "TEXT", label: "test4" },
      { id: "test5", columnType: "TEXT", label: "test5" },
      { id: "test6", columnType: "HEADING", label: "section2" },
      { id: "test7", columnType: "TEXT", label: "test7" },
      { id: "test8", columnType: "TEXT", label: "test8" },
    ];
    expect(toSectionsMap(withSections)).toEqual({
      DEFAULT_SECTION: [
        {
          columnType: "STRING",
          id: "test",
          label: "test",
        },
        {
          columnType: "TEXT",
          id: "test2",
          label: "test2",
        },
      ],
      test3: [
        {
          columnType: "TEXT",
          id: "test4",
          label: "test4",
        },
        {
          columnType: "TEXT",
          id: "test5",
          label: "test5",
        },
      ],
      test6: [
        {
          columnType: "TEXT",
          id: "test7",
          label: "test7",
        },
        {
          columnType: "TEXT",
          id: "test8",
          label: "test8",
        },
      ],
    });
  });
});
