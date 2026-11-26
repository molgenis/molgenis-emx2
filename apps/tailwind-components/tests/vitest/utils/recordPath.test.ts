import { describe, expect, test } from "vitest";
import type {
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
} from "../../../../metadata-utils/src/types";
import type { TableConfig } from "../../../types/recordConfig";
import {
  nestedRecordLink,
  resolveRecordPath,
} from "../../../app/utils/recordPath";

function table(id: string, columns: IColumn[]): ITableMetaData {
  return { id, label: id, tableType: "DATA", schemaId: "demo", columns };
}

const petsColumn: IColumn = {
  id: "pets",
  label: "Pets",
  columnType: "REFBACK",
  refTableId: "Pet",
  refBackId: "owner",
};
const visitsColumn: IColumn = {
  id: "visits",
  label: "Visits",
  columnType: "REFBACK",
  refTableId: "Visit",
  refBackId: "pet",
};
const visitTable = table("Visit", [
  {
    id: "pet",
    label: "Pet",
    columnType: "REF",
    refTableId: "Pet",
    key: 1,
  },
  { id: "date", label: "Date", columnType: "DATE", key: 1 },
  { id: "ward", label: "Ward", columnType: "STRING", key: 1 },
]);

const petTable = table("Pet", [
  {
    id: "owner",
    label: "Owner",
    columnType: "REF",
    refTableId: "Owner",
    key: 1,
  },
  { id: "name", label: "Name", columnType: "STRING", key: 1 },
  visitsColumn,
]);

const schema: ISchemaMetaData = {
  id: "demo",
  label: "demo",
  tables: [
    table("Owner", [
      { id: "name", label: "Name", columnType: "STRING", key: 1 },
      petsColumn,
    ]),
    petTable,
    visitTable,
  ],
};

const configs: Record<string, TableConfig> = {
  Owner: { columns: { pets: { route: "pets" } } },
  Pet: { columns: { visits: { route: "visits" } } },
};

const base = "/display/record/demo";

describe("resolveRecordPath", () => {
  test("a root record is the first segment's table, filtered by its key", () => {
    const resolved = resolveRecordPath(schema, configs, base, ["Owner", "ann"]);
    expect(resolved.tableId).toEqual("Owner");
    expect(resolved.filter).toEqual({ name: { equals: "ann" } });
  });

  test("a nested record is filtered by its own key and by the parent key the path walked through", () => {
    const resolved = resolveRecordPath(schema, configs, base, [
      "Owner",
      "ann",
      "pets",
      "rex",
    ]);
    expect(resolved.tableId).toEqual("Pet");
    expect(resolved.filter).toEqual({
      owner: { name: { equals: "ann" } },
      name: { equals: "rex" },
    });
  });

  test("a composite key splits into its columns, and a comma and a bang inside a value survive", () => {
    const resolved = resolveRecordPath(schema, configs, base, [
      "Owner",
      "ann",
      "pets",
      "rex",
      "visits",
      "2020-01-02,A!c1",
    ]);
    expect(resolved.tableId).toEqual("Visit");
    expect(resolved.filter).toEqual({
      pet: { owner: { name: { equals: "ann" } }, name: { equals: "rex" } },
      date: { equals: "2020-01-02" },
      ward: { equals: "A,1" },
    });
  });

  test("one crumb per segment; the table and route crumbs do not link, record crumbs do", () => {
    const resolved = resolveRecordPath(schema, configs, base, [
      "Owner",
      "ann",
      "pets",
      "rex",
    ]);
    expect(resolved.crumbs).toEqual([
      { label: "Owner", url: "" },
      { label: "ann", url: "/display/record/demo/Owner/ann" },
      { label: "pets", url: "" },
      { label: "rex", url: "/display/record/demo/Owner/ann/pets/rex" },
    ]);
  });

  test.each([
    ["an unknown table", ["Nope", "x"]],
    ["a route no column names", ["Owner", "ann", "cats", "rex"]],
    [
      "a key with too few values",
      ["Owner", "ann", "pets", "rex", "visits", "2020-01-02"],
    ],
    ["a path that ends on a route", ["Owner", "ann", "pets"]],
  ])("rejects %s", (_name, segments) => {
    expect(() => resolveRecordPath(schema, configs, base, segments)).toThrow();
  });
});

describe("nestedRecordLink", () => {
  test("links to the parent path, the route and the key without the column the parent fills", () => {
    expect(
      nestedRecordLink(`${base}/Owner/ann`, petsColumn, "pets", petTable, {
        owner: { name: "ann" },
        name: "rex",
      })
    ).toEqual("/display/record/demo/Owner/ann/pets/rex");
  });

  test("a composite key joins its values with a comma, each value encoded so the path resolves back", () => {
    const to = nestedRecordLink(
      `${base}/Owner/ann/pets/rex`,
      visitsColumn,
      "visits",
      visitTable,
      {
        pet: { owner: { name: "ann" }, name: "rex" },
        date: "2020-01-02",
        ward: "A,1!",
      }
    );
    expect(to).toEqual(
      "/display/record/demo/Owner/ann/pets/rex/visits/2020-01-02,A!c1!!"
    );
    const segments = to
      .replace(`${base}/`, "")
      .split("/")
      .map(decodeURIComponent);
    expect(
      resolveRecordPath(schema, configs, base, segments).filter
    ).toMatchObject({
      date: { equals: "2020-01-02" },
      ward: { equals: "A,1!" },
    });
  });
});
