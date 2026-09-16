import { describe, expect, it } from "vitest";
import { parseMgTableclass } from "../../../app/utils/parseMgTableclass";

describe("parseMgTableclass", () => {
  it("splits schema and table on the first dot", () => {
    expect(parseMgTableclass("schemaA.TableA")).toEqual({
      schemaId: "schemaA",
      tableId: "TableA",
    });
  });

  it("keeps a dot in the table id, splitting only on the first one", () => {
    expect(parseMgTableclass("schemaA.Table.A")).toEqual({
      schemaId: "schemaA",
      tableId: "Table.A",
    });
  });

  it("returns undefined for a non-string value", () => {
    expect(parseMgTableclass(undefined)).toBeUndefined();
    expect(parseMgTableclass(null)).toBeUndefined();
    expect(parseMgTableclass(42)).toBeUndefined();
  });

  it("returns undefined when there is no dot", () => {
    expect(parseMgTableclass("schemaOnly")).toBeUndefined();
  });

  it("returns undefined when the schema half is empty", () => {
    expect(parseMgTableclass(".TableA")).toBeUndefined();
  });

  it("returns undefined when the table half is empty", () => {
    expect(parseMgTableclass("schemaA.")).toBeUndefined();
  });
});
