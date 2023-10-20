import { describe, expect, test } from "vitest";
import { ITableMetaData } from "../../../Interfaces/ITableMetaData";
import constants from "../../constants.js";
import {
  filterVisibleColumns,
  getChapterStyle,
  getRowErrors,
  getSaveDisabledMessage,
  removeKeyColumns,
  splitColumnNamesByHeadings,
} from "./formUtils";
import { IColumn } from "../../../Interfaces/IColumn";
const { AUTO_ID, HEADING } = constants;

describe("getRowErrors", () => {
  test("it should return undefined for an autoId field", () => {
    const rowData = { autoId: "1337" };
    const metaData = {
      columns: [{ name: "autoId", columnType: AUTO_ID }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ autoId: undefined });
  });

  test("it should return undefined for a heading field", () => {
    const rowData = { heading: "1337" };
    const metaData = {
      columns: [{ name: "heading", columnType: HEADING }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ heading: undefined });
  });

  test("it should an error if a required field misses a value", () => {
    const rowData = { required: undefined };
    const metaData = {
      columns: [
        {
          name: "required",
          columnType: "STRING",
          required: true,
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ required: "required is required" });
  });

  test("it should an error if a numerical required field has an invalid value", () => {
    const rowData = { required: NaN };
    const metaData = {
      columns: [
        {
          name: "required",
          columnType: "DECIMAL",
          required: true,
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ required: "required is required" });
  });

  test("it should return undefined it has no value and isn't required", () => {
    const rowData = { empty: null };
    const metaData = {
      columns: [{ name: "empty", columnType: "STRING" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ empty: undefined });
  });

  test("it should return undefined for a valid email address", () => {
    const rowData = { email: "blaat@blabla.bla" };
    const metaData = {
      columns: [{ name: "email", columnType: "EMAIL" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ email: undefined });
  });

  test("it should return an error for an invalid email address", () => {
    const rowData = { email: "in@valid" };
    const metaData = {
      columns: [{ name: "email", columnType: "EMAIL" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ email: "Invalid email address" });
  });

  test("it should return undefined for a valid hyperlink", () => {
    const rowData = { hyperlink: "https://google.com" };
    const metaData = {
      columns: [{ name: "hyperlink", columnType: "HYPERLiNK" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ hyperlink: undefined });
  });

  //FIXME: Hyperlink checking seems to accept anything
  // test("it should return an error for an invalid hyperlink", () => {
  //   const rowData = { hyperlink: "google" };
  //   const metaData = {
  //     columns: [{ name: "hyperlink", columnType: "HYPERLiNK" }],
  //   } as ITableMetaData;
  //   const result = getRowErrors(metaData, rowData);
  //   expect(result).to.deep.equal({ hyperlink: "Invalid hyperlink" });
  // });

  test("it should return undefined for a valid email address array array", () => {
    const rowData = { email: ["blaat@blabla.bla", "bla2@blabla.bla"] };
    const metaData = {
      columns: [{ name: "email", columnType: "EMAIL_ARRAY" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ email: undefined });
  });

  test("it should return an error for an invalid email address array", () => {
    const rowData = { email: ["in@valid", "val@id.com"] };
    const metaData = {
      columns: [{ name: "email", columnType: "EMAIL_ARRAY" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ email: "Invalid email address" });
  });

  test("it should return undefined for a valid hyperlink array", () => {
    const rowData = {
      hyperlink: ["https://google.com", "https://molgenis.org"],
    };
    const metaData = {
      columns: [{ name: "hyperlink", columnType: "HYPERLiNK_ARRAY" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({ hyperlink: undefined });
  });

  //FIXME: Hyperlink checking seems to accept anything
  // test("it should return an error for an invalid hyperlink array ", () => {
  //   const rowData = { hyperlink: ["google"] };
  //   const metaData = {
  //     columns: [{ name: "hyperlink", columnType: "HYPERLiNK_ARRAY" }],
  //   } as ITableMetaData;
  //   const result = getRowErrors(metaData, rowData);
  //   expect(result).to.deep.equal({ hyperlink: "Invalid hyperlink" });
  // });

  test("it should return undefined for a successful validation", () => {
    const rowData = { validation: 2 };
    const metaData = {
      columns: [
        {
          name: "validation",
          columnType: "DECIMAL",
          validation: "validation > 1",
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({
      validation: undefined,
    });
  });

  test("it should return an error for an invalid validation", () => {
    const rowData = { validation: 0 };
    const metaData = {
      columns: [
        {
          name: "validation",
          columnType: "DECIMAL",
          validation: "validation > 1",
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({
      validation: "Applying validation rule returned error: validation > 1",
    });
  });

  test("it should return undefined if there is a reflink with overlap", () => {
    const rowData = { overlap: "refValue", refLink: "refValue" };
    const metaData = {
      columns: [
        {
          name: "overlap",
          columnType: "REF",
          refLink: "refLink",
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({
      overlap: undefined,
    });
  });

  test("it should return an error if there is a reflink without overlap", () => {
    const rowData = { overlap: "refValue", refLink: "refLinkValue" };
    const metaData = {
      columns: [
        {
          name: "overlap",
          columnType: "REF",
          refLink: "refLink",
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({
      overlap: "value should match your selection in column 'refLink'",
    });
  });

  test("it should return an error if there is a reflink array without overlap", () => {
    const rowData = { overlap: ["refValue"], refLink: ["refLinkValue"] };
    const metaData = {
      columns: [
        {
          name: "overlap",
          columnType: "REF",
          refLink: "refLink",
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({
      overlap: "value should match your selection in column 'refLink'",
    });
  });

  test("it should return undefined if there is a reflink array with overlap", () => {
    const rowData = { overlap: ["refValue"], refLink: ["refValue"] };
    const metaData = {
      columns: [
        {
          name: "overlap",
          columnType: "REF",
          refLink: "refLink",
        },
      ],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({
      overlap: undefined,
    });
  });

  test("it should return undefined for a valid input", () => {
    const rowData = { valid: "input " };
    const metaData = {
      columns: [{ name: "valid", columnType: "STRING" }],
    } as ITableMetaData;
    const result = getRowErrors(metaData, rowData);
    expect(result).to.deep.equal({
      valid: undefined,
    });
  });
});

describe("removeKeyColumns", () => {
  test("it should return the data without the data of the key columns", () => {
    const metaData = {
      columns: [{ name: "key", key: 1 }, { name: "some" }],
    } as ITableMetaData;
    const rowData = { some: "Data", key: "primaryKey" };
    const result = removeKeyColumns(metaData, rowData);
    expect(result).toEqual({ some: "Data" });
  });
});

describe("filterVisibleColumns", () => {
  test("it should return the columns if no visisble columns are defined", () => {
    const columns = [{ name: "col1" }, { name: "col2" }] as IColumn[];
    const visibleColumns = null;
    const result = filterVisibleColumns(columns, visibleColumns);
    expect(result).to.deep.equal(columns);
  });

  test("it should return only the visible columns", () => {
    const columns = [{ name: "col1" }, { name: "col2" }] as IColumn[];
    const visibleColumns = ["col2"];
    const result = filterVisibleColumns(columns, visibleColumns);
    expect(result).to.deep.equal([{ name: "col2" }]);
  });
});

describe("splitColumnNamesByHeadings", () => {
  test("it should split all columns by the headings", () => {
    const columns = [
      { name: "heading1", columnType: HEADING },
      { name: "string1", columnType: "STRING" },
      { name: "heading2", columnType: HEADING },
      { name: "string2", columnType: "STRING" },
      { name: "string3", columnType: "STRING" },
    ] as IColumn[];
    const result = splitColumnNamesByHeadings(columns);
    const expectedResult = [
      ["heading1", "string1"],
      ["heading2", "string2", "string3"],
    ];
    expect(result).to.deep.equal(expectedResult);
  });
});

describe("getChapterStyle", () => {
  test("it should return red style for a chapter with errors", () => {
    const page = ["id1", "id2", "id3"];
    const errors = { id1: "some error", id2: undefined };
    const result = getChapterStyle(page, errors);
    expect(result).to.deep.equal({ color: "red" });
  });

  test("it should return an empty style for a chapter without errors", () => {
    const page = ["id1", "id2", "id3"];
    const errors = { id1: undefined, id2: undefined };
    const result = getChapterStyle(page, errors);
    expect(result).to.deep.equal({});
  });
});

describe("getSaveDisabledMessage", () => {
  test("it should return an empty string if saving is possible", () => {
    const rowErrors = {};
    const result = getSaveDisabledMessage(rowErrors);
    expect(result).to.equal("");
  });

  test("it should return string, citing how many errors there are if there are any", () => {
    const rowErrors = { id1: "some error", id2: "another error" };
    const result = getSaveDisabledMessage(rowErrors);
    expect(result).to.equal("There are 2 error(s) preventing saving");
  });
});
