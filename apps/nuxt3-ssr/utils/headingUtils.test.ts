import { describe, it, expect } from "vitest";

import { toHeadings, toSectionsMap } from "./headingUtils";
import { ITableMetaData } from "interfaces/types";

describe("toHeadings", () => {
  it("should return only the heading columns", () => {
    const tableMetaData: ITableMetaData = {
      id: "test",
      name: "test",
      tableType: "test",
      externalSchema: "test",
      columns: [
        { id: "test", columnType: "HEADING", name: "test" },
        { id: "test2", columnType: "TEXT", name: "test2" },
      ],
    };

    expect(toHeadings(tableMetaData)).toEqual([
      { id: "test", columnType: "HEADING", name: "test" },
    ]);
  });

  it("should return empty list in case of no headings", () => {
    const tableMetaData: ITableMetaData = {
      id: "test",
      name: "test",
      tableType: "test",
      externalSchema: "test",
      columns: [],
    };

    expect(toHeadings(tableMetaData)).toEqual([]);
  });
});

describe("toSectionsMap", () => {
  const tableMetaData: ITableMetaData = {
    id: "test",
    name: "test",
    tableType: "test",
    externalSchema: "test",
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
      { id: "test", columnType: "STRING", name: "test" },
      { id: "test2", columnType: "TEXT", name: "test2" },
      { id: "test3", columnType: "HEADING", name: "section1" },
      { id: "test4", columnType: "TEXT", name: "test4" },
      { id: "test5", columnType: "TEXT", name: "test5" },
      { id: "test6", columnType: "HEADING", name: "section2" },
      { id: "test7", columnType: "TEXT", name: "test7" },
      { id: "test8", columnType: "TEXT", name: "test8" },
    ];
    expect(toSectionsMap(withSections)).toEqual({
      DEFAULT_SECTION: [
        {
          columnType: "STRING",
          id: "test",
          name: "test",
        },
        {
          columnType: "TEXT",
          id: "test2",
          name: "test2",
        },
      ],
      test3: [
        {
          columnType: "TEXT",
          id: "test4",
          name: "test4",
        },
        {
          columnType: "TEXT",
          id: "test5",
          name: "test5",
        },
      ],
      test6: [
        {
          columnType: "TEXT",
          id: "test7",
          name: "test7",
        },
        {
          columnType: "TEXT",
          id: "test8",
          name: "test8",
        },
      ],
    });
  });
});
