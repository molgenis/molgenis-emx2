import { expect, it, describe } from "vitest";
import { isFieldDisabled } from "../../../app/utils/isFieldDisabled";

describe("isFieldDisabled", () => {
  it("should return true if column is readonly", () => {
    const rowKey = { rowId: "1" };
    const column = {
      id: "rowId",
      readonly: "true",
      key: 0,
      columnType: "TEXT",
      label: "col label",
    };
    expect(isFieldDisabled(rowKey, column)).toBe(true);
  });

  it("should return true if column is AUTO_ID", () => {
    const rowKey = { rowId: "1" };
    const column = {
      id: "rowId",
      readonly: "false",
      key: 0,
      columnType: "AUTO_ID",
      label: "col label",
    };
    expect(isFieldDisabled(rowKey, column)).toBe(true);
  });

  it("should return true if column is the primary key of the column", () => {
    const rowKey = { rowId: "1" };
    const column = {
      id: "rowId",
      readonly: "false",
      key: 1,
      columnType: "TEXT",
      label: "col label",
    };
    expect(isFieldDisabled(rowKey, column)).toBe(true);
  });

  it("should return false if column is not readonly, not AUTO_ID and not a primary key", () => {
    const rowKey = { rowId: "1" };
    const column = {
      id: "id",
      readonly: "false",
      key: 0,
      columnType: "TEXT",
      label: "col label",
    };
    expect(isFieldDisabled(rowKey, column)).toBe(false);
  });

  it("should return false if column is the primary key of another column", () => {
    const rowKey = { rowId: "0" };
    const column = {
      id: "id",
      readonly: "false",
      key: 1,
      columnType: "TEXT",
      label: "col label",
    };
    expect(isFieldDisabled(rowKey, column)).toBe(false);
  });
});
