import { describe, expect, test } from "vitest";
import type { ITableMetaData } from "../../../../metadata-utils/src/types";
import { buildTableOrderBy } from "../../../app/utils/buildTableOrderBy";

const metadata = {
  id: "Pet",
  columns: [
    { id: "name", key: 1, columnType: "STRING" },
    { id: "owner", key: 1, columnType: "REF" },
    { id: "species", key: 2, columnType: "STRING" },
    { id: "weight", columnType: "DECIMAL" },
  ],
} as unknown as ITableMetaData;

describe("buildTableOrderBy", () => {
  test("sorts on the primary key when no column is selected", () => {
    expect(
      buildTableOrderBy(metadata, { column: "", direction: "ASC" })
    ).toEqual([{ name: "ASC" }, { owner: "ASC" }]);
  });

  test("adds the primary key after the selected column", () => {
    expect(
      buildTableOrderBy(metadata, { column: "weight", direction: "DESC" })
    ).toEqual([{ weight: "DESC" }, { name: "ASC" }, { owner: "ASC" }]);
  });

  test("does not repeat a key column that is the selected column", () => {
    expect(
      buildTableOrderBy(metadata, { column: "name", direction: "DESC" })
    ).toEqual([{ name: "DESC" }, { owner: "ASC" }]);
  });

  test("returns only the selected column without metadata", () => {
    expect(
      buildTableOrderBy(null, { column: "weight", direction: "ASC" })
    ).toEqual([{ weight: "ASC" }]);
  });
});
